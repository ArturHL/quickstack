package com.quickstack.product.service;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService Unit Tests")
class MenuServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private VariantRepository variantRepository;
    @Mock private ModifierGroupRepository modifierGroupRepository;
    @Mock private ModifierRepository modifierRepository;
    @Mock private ComboRepository comboRepository;
    @Mock private ComboItemRepository comboItemRepository;

    @InjectMocks
    private MenuService menuService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Existing tests (updated to stub new repos)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return full menu with multiple categories and products")
    void shouldReturnFullMenuWithCategoriesAndProducts() {
        Category cat1 = category(UUID.randomUUID(), "Tacos", 0);
        Category cat2 = category(UUID.randomUUID(), "Bebidas", 1);

        Product prod1 = simpleProduct(UUID.randomUUID(), "Taco de Bistec", cat1.getId(), true, 0);
        Product prod2 = simpleProduct(UUID.randomUUID(), "Agua", cat2.getId(), true, 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat1, cat2));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(prod1, prod2));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories().get(0).name()).isEqualTo("Tacos");
        assertThat(response.categories().get(1).name()).isEqualTo("Bebidas");
        assertThat(response.categories().get(0).products()).hasSize(1);
        assertThat(response.categories().get(1).products()).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty menu when no categories exist")
    void shouldReturnEmptyMenuWhenNoCategoriesExist() {
        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).isEmpty();
        assertThat(response.combos()).isEmpty();
        verifyNoInteractions(productRepository, variantRepository, modifierGroupRepository, comboRepository);
    }

    @Test
    @DisplayName("Should return empty menu when no active products exist")
    void shouldReturnEmptyMenuWhenNoActiveProductsExist() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).isEmpty();
        assertThat(response.combos()).isEmpty();
        verifyNoInteractions(variantRepository, modifierGroupRepository, comboRepository);
    }

    @Test
    @DisplayName("Should exclude category that has no active products")
    void shouldExcludeCategoryWithNoActiveProducts() {
        Category catWithProducts = category(UUID.randomUUID(), "Tacos", 0);
        Category catEmpty = category(UUID.randomUUID(), "Postres", 1);

        Product prod = simpleProduct(UUID.randomUUID(), "Taco", catWithProducts.getId(), true, 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(catWithProducts, catEmpty));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(prod));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).name()).isEqualTo("Tacos");
    }

    @Test
    @DisplayName("Should include unavailable product marked as out-of-stock")
    void shouldIncludeUnavailableProductMarkedAsOutOfStock() {
        Category cat = category(UUID.randomUUID(), "Bebidas", 0);
        Product available = simpleProduct(UUID.randomUUID(), "Agua", cat.getId(), true, 0);
        Product agotado = simpleProduct(UUID.randomUUID(), "Jugo", cat.getId(), false, 1);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(available, agotado));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).hasSize(1);
        MenuCategoryItem category = response.categories().get(0);
        assertThat(category.products()).hasSize(2);
        assertThat(category.products().get(0).isAvailable()).isTrue();
        assertThat(category.products().get(1).isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should attach variants only to VARIANT-type products")
    void shouldAttachVariantsOnlyToVariantProducts() {
        Category cat = category(UUID.randomUUID(), "Café", 0);

        UUID variantProductId = UUID.randomUUID();
        UUID simpleProductId = UUID.randomUUID();

        Product variantProduct = variantProduct(variantProductId, "Café Americano", cat.getId(), 0);
        Product simpleProduct = simpleProduct(simpleProductId, "Agua", cat.getId(), true, 1);

        ProductVariant chico = variant(UUID.randomUUID(), variantProductId, "Chico", BigDecimal.ZERO, 0);
        ProductVariant grande = variant(UUID.randomUUID(), variantProductId, "Grande", new BigDecimal("10.00"), 1);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(variantProduct, simpleProduct));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of(chico, grande));
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        MenuCategoryItem menuCat = response.categories().get(0);
        assertThat(menuCat.products()).hasSize(2);

        var cafe = menuCat.products().stream().filter(p -> p.name().equals("Café Americano")).findFirst().orElseThrow();
        var agua = menuCat.products().stream().filter(p -> p.name().equals("Agua")).findFirst().orElseThrow();

        assertThat(cafe.variants()).hasSize(2);
        assertThat(cafe.variants().get(0).name()).isEqualTo("Chico");
        assertThat(cafe.variants().get(1).name()).isEqualTo("Grande");
        assertThat(agua.variants()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // New tests: Modifiers integration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should include modifier groups for products that have them")
    void shouldIncludeModifierGroupsForProducts() {
        Category cat = category(UUID.randomUUID(), "Hamburguesas", 0);
        UUID productId = UUID.randomUUID();
        Product product = simpleProduct(productId, "Hamburguesa", cat.getId(), true, 0);

        UUID groupId = UUID.randomUUID();
        ModifierGroup group = modifierGroup(groupId, productId, "Extras", 0, null, false);
        Modifier modifier = modifier(UUID.randomUUID(), groupId, "Extra Queso", new BigDecimal("15.00"), 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of(group));
        when(modifierRepository.findAllByModifierGroupIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of(modifier));
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        var menuProduct = response.categories().get(0).products().get(0);
        assertThat(menuProduct.modifierGroups()).hasSize(1);
        assertThat(menuProduct.modifierGroups().get(0).name()).isEqualTo("Extras");
        assertThat(menuProduct.modifierGroups().get(0).modifiers()).hasSize(1);
        assertThat(menuProduct.modifierGroups().get(0).modifiers().get(0).name()).isEqualTo("Extra Queso");
        assertThat(menuProduct.modifierGroups().get(0).modifiers().get(0).priceAdjustment()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Products without modifier groups return empty modifierGroups list")
    void shouldReturnEmptyModifierGroupsForProductsWithoutGroups() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);
        Product product = simpleProduct(UUID.randomUUID(), "Taco", cat.getId(), true, 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        var menuProduct = response.categories().get(0).products().get(0);
        assertThat(menuProduct.modifierGroups()).isEmpty();
        // modifiers repo should NOT be called when there are no groups
        verifyNoInteractions(modifierRepository);
    }

    @Test
    @DisplayName("Modifier groups with no active modifiers return empty modifiers list")
    void shouldReturnEmptyModifiersForGroupWithNoActiveModifiers() {
        Category cat = category(UUID.randomUUID(), "Pizzas", 0);
        UUID productId = UUID.randomUUID();
        Product product = simpleProduct(productId, "Pizza Margherita", cat.getId(), true, 0);

        UUID groupId = UUID.randomUUID();
        ModifierGroup group = modifierGroup(groupId, productId, "Ingredientes Extra", 0, 5, false);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of(group));
        when(modifierRepository.findAllByModifierGroupIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        var menuGroup = response.categories().get(0).products().get(0).modifierGroups().get(0);
        assertThat(menuGroup.modifiers()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // New tests: Combos integration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should include active combos in menu response")
    void shouldIncludeCombosInMenuResponse() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);
        UUID productId = UUID.randomUUID();
        Product product = simpleProduct(productId, "Taco", cat.getId(), true, 0);

        Combo combo = combo(UUID.randomUUID(), "Combo Familiar", new BigDecimal("99.00"), 0);
        ComboItem comboItem = comboItem(combo.getId(), productId, 2);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(combo));
        when(comboItemRepository.findAllByTenantIdAndComboIdIn(eq(tenantId), anyCollection())).thenReturn(List.of(comboItem));

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.combos()).hasSize(1);
        assertThat(response.combos().get(0).name()).isEqualTo("Combo Familiar");
        assertThat(response.combos().get(0).price()).isEqualByComparingTo("99.00");
        assertThat(response.combos().get(0).items()).hasSize(1);
        assertThat(response.combos().get(0).items().get(0).productName()).isEqualTo("Taco");
        assertThat(response.combos().get(0).items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should exclude combo when any of its products is not in the active products list")
    void shouldExcludeComboWithInactiveProduct() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);
        UUID activeProductId = UUID.randomUUID();
        Product activeProduct = simpleProduct(activeProductId, "Taco", cat.getId(), true, 0);
        UUID inactiveProductId = UUID.randomUUID(); // not in products list (inactive/deleted)

        Combo combo = combo(UUID.randomUUID(), "Combo Mixto", new BigDecimal("89.00"), 0);
        ComboItem item1 = comboItem(combo.getId(), activeProductId, 1);
        ComboItem item2 = comboItem(combo.getId(), inactiveProductId, 1); // product not in active list

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(activeProduct));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(combo));
        when(comboItemRepository.findAllByTenantIdAndComboIdIn(eq(tenantId), anyCollection())).thenReturn(List.of(item1, item2));

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.combos()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty combos list when no active combos exist")
    void shouldReturnEmptyCombosWhenNone() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);
        Product product = simpleProduct(UUID.randomUUID(), "Taco", cat.getId(), true, 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.combos()).isEmpty();
        verifyNoInteractions(comboItemRepository);
    }

    @Test
    @DisplayName("Should exclude combo with empty items list")
    void shouldExcludeComboWithNoItems() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);
        Product product = simpleProduct(UUID.randomUUID(), "Taco", cat.getId(), true, 0);

        Combo combo = combo(UUID.randomUUID(), "Combo Vacío", new BigDecimal("50.00"), 0);
        // No items for this combo

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(product));
        when(variantRepository.findAllActiveByTenantId(tenantId)).thenReturn(List.of());
        when(modifierGroupRepository.findAllByProductIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(comboRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(combo));
        when(comboItemRepository.findAllByTenantIdAndComboIdIn(eq(tenantId), anyCollection())).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.combos()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category category(UUID id, String name, int sortOrder) {
        Category c = new Category();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setName(name);
        c.setSortOrder(sortOrder);
        c.setActive(true);
        return c;
    }

    private Product simpleProduct(UUID id, String name, UUID categoryId, boolean isAvailable, int sortOrder) {
        Product p = new Product();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setCategoryId(categoryId);
        p.setName(name);
        p.setBasePrice(new BigDecimal("25.00"));
        p.setProductType(ProductType.SIMPLE);
        p.setActive(true);
        p.setAvailable(isAvailable);
        p.setSortOrder(sortOrder);
        return p;
    }

    private Product variantProduct(UUID id, String name, UUID categoryId, int sortOrder) {
        Product p = new Product();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setCategoryId(categoryId);
        p.setName(name);
        p.setBasePrice(new BigDecimal("50.00"));
        p.setProductType(ProductType.VARIANT);
        p.setActive(true);
        p.setAvailable(true);
        p.setSortOrder(sortOrder);
        return p;
    }

    private ProductVariant variant(UUID id, UUID productId, String name, BigDecimal priceAdj, int sortOrder) {
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setTenantId(tenantId);
        v.setProductId(productId);
        v.setName(name);
        v.setPriceAdjustment(priceAdj);
        v.setSortOrder(sortOrder);
        return v;
    }

    private ModifierGroup modifierGroup(UUID id, UUID productId, String name,
                                         int minSel, Integer maxSel, boolean isRequired) {
        ModifierGroup g = new ModifierGroup();
        g.setId(id);
        g.setTenantId(tenantId);
        g.setProductId(productId);
        g.setName(name);
        g.setMinSelections(minSel);
        g.setMaxSelections(maxSel);
        g.setRequired(isRequired);
        g.setSortOrder(0);
        return g;
    }

    private Modifier modifier(UUID id, UUID groupId, String name, BigDecimal priceAdj, int sortOrder) {
        Modifier m = new Modifier();
        m.setId(id);
        m.setTenantId(tenantId);
        m.setModifierGroupId(groupId);
        m.setName(name);
        m.setPriceAdjustment(priceAdj);
        m.setDefault(false);
        m.setActive(true);
        m.setSortOrder(sortOrder);
        return m;
    }

    private Combo combo(UUID id, String name, BigDecimal price, int sortOrder) {
        Combo c = new Combo();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setName(name);
        c.setPrice(price);
        c.setActive(true);
        c.setSortOrder(sortOrder);
        return c;
    }

    private ComboItem comboItem(UUID comboId, UUID productId, int quantity) {
        ComboItem ci = new ComboItem();
        ci.setId(UUID.randomUUID());
        ci.setTenantId(tenantId);
        ci.setComboId(comboId);
        ci.setProductId(productId);
        ci.setQuantity(quantity);
        ci.setSortOrder(0);
        return ci;
    }
}
