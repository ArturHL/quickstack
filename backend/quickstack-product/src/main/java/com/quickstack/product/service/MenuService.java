package com.quickstack.product.service;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuComboItem;
import com.quickstack.product.dto.response.MenuComboItem.ComboProductEntry;
import com.quickstack.product.dto.response.MenuModifierGroupItem;
import com.quickstack.product.dto.response.MenuModifierItem;
import com.quickstack.product.dto.response.MenuProductItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.dto.response.MenuVariantItem;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.CategoryRepository;
import com.quickstack.product.repository.ComboItemRepository;
import com.quickstack.product.repository.ComboRepository;
import com.quickstack.product.repository.ModifierGroupRepository;
import com.quickstack.product.repository.ModifierRepository;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for building the public menu view consumed by the POS screen.
 * <p>
 * Uses at most 7 flat database queries to avoid N+1:
 * - Q1: all active categories for the tenant, ordered by sort_order
 * - Q2: all active products for the tenant, ordered by sort_order
 * - Q3: all active variants for the tenant, ordered by sort_order
 * - Q4: all modifier groups for the tenant's active products (batch)
 * - Q5: all active modifiers for those groups (batch)
 * - Q6: all active combos for the tenant
 * - Q7: all combo items for those combos (batch)
 * Results are assembled in memory.
 * <p>
 * Business rules:
 * - Only active (is_active=true) products appear in the menu
 * - Unavailable (is_available=false) products are included but marked as out-of-stock
 * - Categories with no active products are excluded from the response
 * - Variants are included only for VARIANT-type products
 * - Combos are excluded if any of their products are inactive or missing
 * - Combos appear as a top-level list (they are tenant-scoped, not category-scoped)
 */
@Service
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;

    public MenuService(CategoryRepository categoryRepository,
                       ProductRepository productRepository,
                       VariantRepository variantRepository,
                       ModifierGroupRepository modifierGroupRepository,
                       ModifierRepository modifierRepository,
                       ComboRepository comboRepository,
                       ComboItemRepository comboItemRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.comboRepository = comboRepository;
        this.comboItemRepository = comboItemRepository;
    }

    /**
     * Returns the full menu for the given tenant, organized by category, with combos
     * as a top-level list.
     * <p>
     * Empty categories (no active products) are excluded from the result.
     * Combos where any product is inactive are excluded.
     *
     * @param tenantId the tenant whose menu to build
     * @return the complete menu response
     */
    @Transactional(readOnly = true)
    public MenuResponse getMenu(UUID tenantId) {
        // Q1: active categories ordered by sort_order
        List<Category> categories = categoryRepository.findAllActiveForMenuByTenantId(tenantId);
        if (categories.isEmpty()) {
            return MenuResponse.empty();
        }

        // Q2: all active products for tenant ordered by sort_order
        List<Product> products = productRepository.findAllActiveForMenuByTenantId(tenantId);
        if (products.isEmpty()) {
            return MenuResponse.empty();
        }

        // Q3: all active variants for tenant ordered by sort_order
        List<ProductVariant> allVariants = variantRepository.findAllActiveByTenantId(tenantId);

        // Q4 + Q5: modifier groups and their modifiers (batch, no N+1)
        Map<UUID, List<MenuModifierGroupItem>> modifierGroupsByProductId =
                loadModifierGroupsByProduct(products, tenantId);

        // Q6 + Q7: active combos and their items (batch, no N+1)
        Map<UUID, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        List<MenuComboItem> menuCombos = loadCombos(tenantId, productsById);

        // Group variants by productId for O(1) lookup
        Map<UUID, List<ProductVariant>> variantsByProductId = allVariants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        // Group products by categoryId for O(1) lookup
        Map<UUID, List<Product>> productsByCategoryId = products.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId));

        // Assemble the menu, skipping empty categories
        List<MenuCategoryItem> menuCategories = categories.stream()
                .filter(category -> productsByCategoryId.containsKey(category.getId()))
                .map(category -> buildCategoryItem(
                        category,
                        productsByCategoryId.getOrDefault(category.getId(), List.of()),
                        variantsByProductId,
                        modifierGroupsByProductId
                ))
                .toList();

        return MenuResponse.of(menuCategories, menuCombos);
    }

    /**
     * Loads all modifier groups and their modifiers for the given products in two batch queries.
     *
     * @return map of productId → list of MenuModifierGroupItem (already assembled with modifiers)
     */
    private Map<UUID, List<MenuModifierGroupItem>> loadModifierGroupsByProduct(
            List<Product> products, UUID tenantId) {

        List<UUID> productIds = products.stream().map(Product::getId).toList();

        // Q4: batch-load all modifier groups for these products
        List<ModifierGroup> allGroups = modifierGroupRepository.findAllByProductIdInAndTenantId(productIds, tenantId);
        if (allGroups.isEmpty()) {
            return Map.of();
        }

        // Q5: batch-load all active modifiers for these groups
        List<UUID> groupIds = allGroups.stream().map(ModifierGroup::getId).toList();
        List<Modifier> allModifiers = modifierRepository.findAllByModifierGroupIdInAndTenantId(groupIds, tenantId);

        Map<UUID, List<Modifier>> modifiersByGroupId = allModifiers.stream()
                .collect(Collectors.groupingBy(Modifier::getModifierGroupId));

        // Assemble: productId → [MenuModifierGroupItem, ...]
        Map<UUID, List<MenuModifierGroupItem>> result = new HashMap<>();
        for (ModifierGroup group : allGroups) {
            List<MenuModifierItem> menuModifiers = modifiersByGroupId
                    .getOrDefault(group.getId(), List.of())
                    .stream()
                    .map(MenuModifierItem::from)
                    .toList();

            result.computeIfAbsent(group.getProductId(), k -> new ArrayList<>())
                    .add(MenuModifierGroupItem.from(group, menuModifiers));
        }
        return result;
    }

    /**
     * Loads active combos and filters out any whose products are not all active.
     *
     * @param tenantId   the tenant ID
     * @param productsById map of productId → Product (already loaded, active products only)
     * @return list of MenuComboItem ready for the response
     */
    private List<MenuComboItem> loadCombos(UUID tenantId, Map<UUID, Product> productsById) {
        // Q6: active combos
        List<Combo> activeCombos = comboRepository.findAllActiveForMenuByTenantId(tenantId);
        if (activeCombos.isEmpty()) {
            return List.of();
        }

        // Q7: batch-load all combo items
        List<UUID> comboIds = activeCombos.stream().map(Combo::getId).toList();
        List<ComboItem> allComboItems = comboItemRepository.findAllByTenantIdAndComboIdIn(tenantId, comboIds);

        Map<UUID, List<ComboItem>> comboItemsByComboId = allComboItems.stream()
                .collect(Collectors.groupingBy(ComboItem::getComboId));

        return activeCombos.stream()
                .filter(combo -> isComboFullyActive(combo, comboItemsByComboId, productsById))
                .map(combo -> buildComboItem(
                        combo,
                        comboItemsByComboId.getOrDefault(combo.getId(), List.of()),
                        productsById))
                .toList();
    }

    /**
     * Returns true if the combo has items and all its products are in the active products map.
     */
    private boolean isComboFullyActive(Combo combo,
                                        Map<UUID, List<ComboItem>> comboItemsByComboId,
                                        Map<UUID, Product> productsById) {
        List<ComboItem> items = comboItemsByComboId.getOrDefault(combo.getId(), List.of());
        if (items.isEmpty()) {
            return false;
        }
        return items.stream().allMatch(item -> productsById.containsKey(item.getProductId()));
    }

    private MenuComboItem buildComboItem(Combo combo,
                                          List<ComboItem> items,
                                          Map<UUID, Product> productsById) {
        List<ComboProductEntry> entries = items.stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    String productName = product != null ? product.getName() : "Unknown";
                    return new ComboProductEntry(item.getProductId(), productName, item.getQuantity());
                })
                .toList();
        return MenuComboItem.from(combo, entries);
    }

    private MenuCategoryItem buildCategoryItem(Category category,
                                                List<Product> products,
                                                Map<UUID, List<ProductVariant>> variantsByProductId,
                                                Map<UUID, List<MenuModifierGroupItem>> modifierGroupsByProductId) {
        List<MenuProductItem> menuProducts = products.stream()
                .map(product -> buildProductItem(product, variantsByProductId, modifierGroupsByProductId))
                .toList();
        return MenuCategoryItem.from(category, menuProducts);
    }

    private MenuProductItem buildProductItem(Product product,
                                              Map<UUID, List<ProductVariant>> variantsByProductId,
                                              Map<UUID, List<MenuModifierGroupItem>> modifierGroupsByProductId) {
        List<MenuVariantItem> variants = List.of();
        if (product.getProductType() == ProductType.VARIANT) {
            variants = variantsByProductId.getOrDefault(product.getId(), List.of())
                    .stream()
                    .map(v -> MenuVariantItem.from(v, product.getBasePrice()))
                    .toList();
        }
        List<MenuModifierGroupItem> modifierGroups =
                modifierGroupsByProductId.getOrDefault(product.getId(), List.of());

        return MenuProductItem.from(product, variants, modifierGroups);
    }
}
