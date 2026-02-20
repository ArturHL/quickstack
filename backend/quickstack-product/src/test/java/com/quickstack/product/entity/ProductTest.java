package com.quickstack.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Product entity.
 */
@DisplayName("Product Entity")
class ProductTest {

    @Test
    @DisplayName("Should create simple product with base price")
    void shouldCreateSimpleProduct() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String name = "Taco al Pastor";
        BigDecimal basePrice = new BigDecimal("45.00");

        // When
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(basePrice);
        product.setProductType(ProductType.SIMPLE);

        // Then
        assertThat(product.getTenantId()).isEqualTo(tenantId);
        assertThat(product.getCategoryId()).isEqualTo(categoryId);
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getBasePrice()).isEqualByComparingTo(basePrice);
        assertThat(product.getProductType()).isEqualTo(ProductType.SIMPLE);
        assertThat(product.isActive()).isTrue(); // Default
        assertThat(product.isAvailable()).isTrue(); // Default
    }

    @Test
    @DisplayName("Should get effective price for SIMPLE product")
    void shouldGetEffectivePriceForSimpleProduct() {
        // Given
        Product product = new Product();
        product.setProductType(ProductType.SIMPLE);
        product.setBasePrice(new BigDecimal("45.00"));

        // When
        BigDecimal effectivePrice = product.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    @DisplayName("Should return null effective price for VARIANT product")
    void shouldReturnNullEffectivePriceForVariantProduct() {
        // Given
        Product product = new Product();
        product.setProductType(ProductType.VARIANT);
        product.setBasePrice(new BigDecimal("30.00"));

        // When
        BigDecimal effectivePrice = product.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isNull();
    }

    @Test
    @DisplayName("Should return null effective price for COMBO product")
    void shouldReturnNullEffectivePriceForComboProduct() {
        // Given
        Product product = new Product();
        product.setProductType(ProductType.COMBO);
        product.setBasePrice(new BigDecimal("99.00"));

        // When
        BigDecimal effectivePrice = product.getEffectivePrice();

        // Then
        assertThat(effectivePrice).isNull();
    }

    @Test
    @DisplayName("Should manage variants collection")
    void shouldManageVariantsCollection() {
        // Given
        Product product = new Product();
        product.setProductType(ProductType.VARIANT);

        ProductVariant smallVariant = new ProductVariant();
        smallVariant.setName("Chico");
        smallVariant.setPriceAdjustment(BigDecimal.ZERO);

        ProductVariant largeVariant = new ProductVariant();
        largeVariant.setName("Grande");
        largeVariant.setPriceAdjustment(new BigDecimal("15.00"));

        List<ProductVariant> variants = new ArrayList<>();
        variants.add(smallVariant);
        variants.add(largeVariant);

        // When
        product.setVariants(variants);

        // Then
        assertThat(product.getVariants()).hasSize(2);
        assertThat(product.getVariants()).contains(smallVariant, largeVariant);
    }

    @Test
    @DisplayName("Should identify as deleted when deletedAt is set")
    void shouldBeDeletedWhenDeletedAtIsSet() {
        // Given
        Product product = new Product();
        product.setDeletedAt(Instant.now());

        // When
        boolean isDeleted = product.isDeleted();

        // Then
        assertThat(isDeleted).isTrue();
    }

    @Test
    @DisplayName("Should identify as not deleted when deletedAt is null")
    void shouldNotBeDeletedWhenDeletedAtIsNull() {
        // Given
        Product product = new Product();
        product.setDeletedAt(null);

        // When
        boolean isDeleted = product.isDeleted();

        // Then
        assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("Should support SKU for inventory management")
    void shouldSupportSku() {
        // Given
        Product product = new Product();
        String sku = "TACO-PASTOR-001";

        // When
        product.setSku(sku);

        // Then
        assertThat(product.getSku()).isEqualTo(sku);
    }

    @Test
    @DisplayName("Should track cost price for margin calculations")
    void shouldTrackCostPrice() {
        // Given
        Product product = new Product();
        BigDecimal basePrice = new BigDecimal("45.00");
        BigDecimal costPrice = new BigDecimal("25.00");

        // When
        product.setBasePrice(basePrice);
        product.setCostPrice(costPrice);

        // Then
        assertThat(product.getCostPrice()).isEqualByComparingTo(costPrice);
        assertThat(product.getBasePrice()).isEqualByComparingTo(basePrice);
    }

    @Test
    @DisplayName("Should default to not tracking inventory")
    void shouldDefaultToNotTrackingInventory() {
        // Given
        Product product = new Product();

        // Then
        assertThat(product.isTrackInventory()).isFalse();
    }
}
