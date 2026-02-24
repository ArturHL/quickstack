package com.quickstack.product.service;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuProductItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.dto.response.MenuVariantItem;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.CategoryRepository;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for building the public menu view consumed by the POS screen.
 * <p>
 * Uses exactly 2 database queries to avoid N+1:
 * - Query 1: all active categories for the tenant, ordered by sort_order
 * - Query 2: all active products for the tenant, ordered by sort_order
 * - Query 3: all active variants for the tenant, ordered by sort_order
 * Results are assembled in memory.
 * <p>
 * Business rules:
 * - Only active (is_active=true) products appear in the menu
 * - Unavailable (is_available=false) products are included but marked as out-of-stock
 * - Categories with no active products are excluded from the response
 * - Variants are included only for VARIANT-type products
 */
@Service
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;

    public MenuService(CategoryRepository categoryRepository,
                       ProductRepository productRepository,
                       VariantRepository variantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
    }

    /**
     * Returns the full menu for the given tenant, organized by category.
     * <p>
     * Empty categories (no active products) are excluded from the result.
     *
     * @param tenantId the tenant whose menu to build
     * @return the complete menu response
     */
    @Transactional(readOnly = true)
    public MenuResponse getMenu(UUID tenantId) {
        // Query 1: active categories ordered by sort_order
        List<Category> categories = categoryRepository.findAllActiveForMenuByTenantId(tenantId);
        if (categories.isEmpty()) {
            return MenuResponse.empty();
        }

        // Query 2: all active products for tenant ordered by sort_order
        List<Product> products = productRepository.findAllActiveForMenuByTenantId(tenantId);
        if (products.isEmpty()) {
            return MenuResponse.empty();
        }

        // Query 3: all active variants for tenant ordered by sort_order
        List<ProductVariant> allVariants = variantRepository.findAllActiveByTenantId(tenantId);

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
                variantsByProductId
            ))
            .toList();

        return MenuResponse.of(menuCategories);
    }

    private MenuCategoryItem buildCategoryItem(Category category,
                                               List<Product> products,
                                               Map<UUID, List<ProductVariant>> variantsByProductId) {
        List<MenuProductItem> menuProducts = products.stream()
            .map(product -> buildProductItem(product, variantsByProductId))
            .toList();

        return MenuCategoryItem.from(category, menuProducts);
    }

    private MenuProductItem buildProductItem(Product product,
                                             Map<UUID, List<ProductVariant>> variantsByProductId) {
        List<MenuVariantItem> variants = List.of();
        if (product.getProductType() == ProductType.VARIANT) {
            variants = variantsByProductId.getOrDefault(product.getId(), List.of())
                .stream()
                .map(v -> MenuVariantItem.from(v, product.getBasePrice()))
                .toList();
        }
        return MenuProductItem.from(product, variants);
    }
}
