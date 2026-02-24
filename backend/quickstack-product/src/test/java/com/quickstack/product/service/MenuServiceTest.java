package com.quickstack.product.service;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.CategoryRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService Unit Tests")
class MenuServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VariantRepository variantRepository;

    @InjectMocks
    private MenuService menuService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

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
        verifyNoInteractions(productRepository, variantRepository);
    }

    @Test
    @DisplayName("Should return empty menu when no active products exist")
    void shouldReturnEmptyMenuWhenNoActiveProductsExist() {
        Category cat = category(UUID.randomUUID(), "Tacos", 0);

        when(categoryRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of(cat));
        when(productRepository.findAllActiveForMenuByTenantId(tenantId)).thenReturn(List.of());

        MenuResponse response = menuService.getMenu(tenantId);

        assertThat(response.categories()).isEmpty();
        verifyNoInteractions(variantRepository);
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
}
