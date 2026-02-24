package com.quickstack.product.dto;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuProductItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.dto.response.MenuVariantItem;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Menu DTOs Unit Tests")
class MenuResponseTest {

    @Test
    @DisplayName("MenuResponse.empty() returns response with empty category list")
    void menuResponseEmptyReturnsEmptyCategories() {
        MenuResponse response = MenuResponse.empty();

        assertThat(response.categories()).isEmpty();
    }

    @Test
    @DisplayName("MenuVariantItem.from() maps variant fields and calculates effectivePrice")
    void menuVariantItemFromMapsFieldsCorrectly() {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setName("Grande");
        variant.setPriceAdjustment(new BigDecimal("10.00"));
        variant.setDefault(true);
        variant.setSortOrder(2);

        BigDecimal basePrice = new BigDecimal("50.00");
        MenuVariantItem item = MenuVariantItem.from(variant, basePrice);

        assertThat(item.id()).isEqualTo(variant.getId());
        assertThat(item.name()).isEqualTo("Grande");
        assertThat(item.priceAdjustment()).isEqualByComparingTo("10.00");
        assertThat(item.effectivePrice()).isEqualByComparingTo("60.00");
        assertThat(item.isDefault()).isTrue();
        assertThat(item.sortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("MenuProductItem.from() maps product fields; variants list is empty for SIMPLE product")
    void menuProductItemFromMapsSimpleProductCorrectly() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Taco");
        product.setBasePrice(new BigDecimal("25.00"));
        product.setImageUrl("https://example.com/taco.jpg");
        product.setAvailable(false);
        product.setProductType(ProductType.SIMPLE);

        MenuProductItem item = MenuProductItem.from(product, List.of());

        assertThat(item.id()).isEqualTo(product.getId());
        assertThat(item.name()).isEqualTo("Taco");
        assertThat(item.basePrice()).isEqualByComparingTo("25.00");
        assertThat(item.imageUrl()).isEqualTo("https://example.com/taco.jpg");
        assertThat(item.isAvailable()).isFalse();
        assertThat(item.productType()).isEqualTo(ProductType.SIMPLE);
        assertThat(item.variants()).isEmpty();
    }

    @Test
    @DisplayName("MenuCategoryItem.from() maps category fields and includes product list")
    void menuCategoryItemFromMapsCategoryCorrectly() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Bebidas");
        category.setSortOrder(1);
        category.setImageUrl("https://example.com/bebidas.jpg");

        MenuProductItem product = new MenuProductItem(
            UUID.randomUUID(), "Agua", new BigDecimal("15.00"),
            null, true, ProductType.SIMPLE, List.of()
        );

        MenuCategoryItem item = MenuCategoryItem.from(category, List.of(product));

        assertThat(item.id()).isEqualTo(category.getId());
        assertThat(item.name()).isEqualTo("Bebidas");
        assertThat(item.sortOrder()).isEqualTo(1);
        assertThat(item.imageUrl()).isEqualTo("https://example.com/bebidas.jpg");
        assertThat(item.products()).hasSize(1);
        assertThat(item.products().get(0).name()).isEqualTo("Agua");
    }

    @Test
    @DisplayName("MenuResponse.of() assembles full hierarchical structure")
    void menuResponseOfAssemblesFullHierarchy() {
        UUID catId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        UUID varId = UUID.randomUUID();

        MenuVariantItem variant = new MenuVariantItem(
            varId, "Mediano", new BigDecimal("5.00"), new BigDecimal("55.00"), true, 1
        );
        MenuProductItem product = new MenuProductItem(
            prodId, "Café", new BigDecimal("50.00"), null, true, ProductType.VARIANT, List.of(variant)
        );
        MenuCategoryItem category = new MenuCategoryItem(
            catId, "Bebidas Calientes", 0, null, List.of(product)
        );

        MenuResponse response = MenuResponse.of(List.of(category));

        assertThat(response.categories()).hasSize(1);
        MenuCategoryItem cat = response.categories().get(0);
        assertThat(cat.id()).isEqualTo(catId);
        assertThat(cat.products()).hasSize(1);
        MenuProductItem prod = cat.products().get(0);
        assertThat(prod.productType()).isEqualTo(ProductType.VARIANT);
        assertThat(prod.variants()).hasSize(1);
        assertThat(prod.variants().get(0).effectivePrice()).isEqualByComparingTo("55.00");
    }
}
