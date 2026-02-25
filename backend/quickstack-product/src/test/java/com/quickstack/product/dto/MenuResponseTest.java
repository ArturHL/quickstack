package com.quickstack.product.dto;

import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuComboItem;
import com.quickstack.product.dto.response.MenuModifierGroupItem;
import com.quickstack.product.dto.response.MenuModifierItem;
import com.quickstack.product.dto.response.MenuProductItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.dto.response.MenuVariantItem;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
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
    @DisplayName("MenuResponse.empty() returns response with empty categories and combos")
    void menuResponseEmptyReturnsEmptyCategoriesAndCombos() {
        MenuResponse response = MenuResponse.empty();

        assertThat(response.categories()).isEmpty();
        assertThat(response.combos()).isEmpty();
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
    @DisplayName("MenuProductItem.from() maps product fields; variants and modifierGroups empty for SIMPLE product")
    void menuProductItemFromMapsSimpleProductCorrectly() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Taco");
        product.setBasePrice(new BigDecimal("25.00"));
        product.setImageUrl("https://example.com/taco.jpg");
        product.setAvailable(false);
        product.setProductType(ProductType.SIMPLE);

        MenuProductItem item = MenuProductItem.from(product, List.of(), List.of());

        assertThat(item.id()).isEqualTo(product.getId());
        assertThat(item.name()).isEqualTo("Taco");
        assertThat(item.basePrice()).isEqualByComparingTo("25.00");
        assertThat(item.imageUrl()).isEqualTo("https://example.com/taco.jpg");
        assertThat(item.isAvailable()).isFalse();
        assertThat(item.productType()).isEqualTo(ProductType.SIMPLE);
        assertThat(item.variants()).isEmpty();
        assertThat(item.modifierGroups()).isEmpty();
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
            null, true, ProductType.SIMPLE, List.of(), List.of()
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
    @DisplayName("MenuResponse.of() assembles full hierarchical structure with combos")
    void menuResponseOfAssemblesFullHierarchyWithCombos() {
        UUID catId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        UUID varId = UUID.randomUUID();

        MenuVariantItem variant = new MenuVariantItem(
            varId, "Mediano", new BigDecimal("5.00"), new BigDecimal("55.00"), true, 1
        );
        MenuProductItem product = new MenuProductItem(
            prodId, "Café", new BigDecimal("50.00"), null, true, ProductType.VARIANT, List.of(variant), List.of()
        );
        MenuCategoryItem category = new MenuCategoryItem(
            catId, "Bebidas Calientes", 0, null, List.of(product)
        );

        MenuComboItem combo = new MenuComboItem(
            UUID.randomUUID(), "Combo Desayuno", null, null, new BigDecimal("99.00"), 0, List.of()
        );

        MenuResponse response = MenuResponse.of(List.of(category), List.of(combo));

        assertThat(response.categories()).hasSize(1);
        MenuCategoryItem cat = response.categories().get(0);
        assertThat(cat.id()).isEqualTo(catId);
        assertThat(cat.products()).hasSize(1);
        MenuProductItem prod = cat.products().get(0);
        assertThat(prod.productType()).isEqualTo(ProductType.VARIANT);
        assertThat(prod.variants()).hasSize(1);
        assertThat(prod.variants().get(0).effectivePrice()).isEqualByComparingTo("55.00");
        assertThat(response.combos()).hasSize(1);
        assertThat(response.combos().get(0).name()).isEqualTo("Combo Desayuno");
    }

    // -------------------------------------------------------------------------
    // New DTO tests for Sprint 4
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MenuModifierItem.from() maps modifier fields correctly")
    void menuModifierItemFromMapsFieldsCorrectly() {
        Modifier modifier = new Modifier();
        modifier.setId(UUID.randomUUID());
        modifier.setName("Extra Queso");
        modifier.setPriceAdjustment(new BigDecimal("15.00"));
        modifier.setDefault(false);
        modifier.setSortOrder(1);

        MenuModifierItem item = MenuModifierItem.from(modifier);

        assertThat(item.id()).isEqualTo(modifier.getId());
        assertThat(item.name()).isEqualTo("Extra Queso");
        assertThat(item.priceAdjustment()).isEqualByComparingTo("15.00");
        assertThat(item.isDefault()).isFalse();
        assertThat(item.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("MenuModifierGroupItem.from() maps group fields with modifiers")
    void menuModifierGroupItemFromMapsFieldsCorrectly() {
        ModifierGroup group = new ModifierGroup();
        group.setId(UUID.randomUUID());
        group.setName("Extras");
        group.setMinSelections(0);
        group.setMaxSelections(3);
        group.setRequired(false);

        MenuModifierItem modifierItem = new MenuModifierItem(
            UUID.randomUUID(), "Extra Queso", new BigDecimal("15.00"), false, 0
        );

        MenuModifierGroupItem item = MenuModifierGroupItem.from(group, List.of(modifierItem));

        assertThat(item.id()).isEqualTo(group.getId());
        assertThat(item.name()).isEqualTo("Extras");
        assertThat(item.minSelections()).isEqualTo(0);
        assertThat(item.maxSelections()).isEqualTo(3);
        assertThat(item.isRequired()).isFalse();
        assertThat(item.modifiers()).hasSize(1);
        assertThat(item.modifiers().get(0).name()).isEqualTo("Extra Queso");
    }

    @Test
    @DisplayName("MenuModifierGroupItem.from() maps required group with null maxSelections")
    void menuModifierGroupItemFromMapsRequiredGroupWithNullMax() {
        ModifierGroup group = new ModifierGroup();
        group.setId(UUID.randomUUID());
        group.setName("Tamaño de Bebida");
        group.setMinSelections(1);
        group.setMaxSelections(null);
        group.setRequired(true);

        MenuModifierGroupItem item = MenuModifierGroupItem.from(group, List.of());

        assertThat(item.isRequired()).isTrue();
        assertThat(item.minSelections()).isEqualTo(1);
        assertThat(item.maxSelections()).isNull();
    }

    @Test
    @DisplayName("MenuComboItem.from() maps combo fields with product entries")
    void menuComboItemFromMapsFieldsCorrectly() {
        Combo combo = new Combo();
        combo.setId(UUID.randomUUID());
        combo.setName("Combo 1");
        combo.setDescription("Hamburguesa + Papas + Refresco");
        combo.setImageUrl("https://example.com/combo1.jpg");
        combo.setPrice(new BigDecimal("99.00"));
        combo.setSortOrder(0);

        UUID productId = UUID.randomUUID();
        MenuComboItem.ComboProductEntry entry = new MenuComboItem.ComboProductEntry(
            productId, "Hamburguesa", 1
        );

        MenuComboItem item = MenuComboItem.from(combo, List.of(entry));

        assertThat(item.id()).isEqualTo(combo.getId());
        assertThat(item.name()).isEqualTo("Combo 1");
        assertThat(item.description()).isEqualTo("Hamburguesa + Papas + Refresco");
        assertThat(item.imageUrl()).isEqualTo("https://example.com/combo1.jpg");
        assertThat(item.price()).isEqualByComparingTo("99.00");
        assertThat(item.sortOrder()).isEqualTo(0);
        assertThat(item.items()).hasSize(1);
        assertThat(item.items().get(0).productName()).isEqualTo("Hamburguesa");
        assertThat(item.items().get(0).quantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("MenuProductItem includes modifier groups for products with customizations")
    void menuProductItemIncludesModifierGroups() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Hamburguesa");
        product.setBasePrice(new BigDecimal("80.00"));
        product.setProductType(ProductType.SIMPLE);
        product.setAvailable(true);

        MenuModifierItem modifierItem = new MenuModifierItem(
            UUID.randomUUID(), "Extra Queso", new BigDecimal("15.00"), false, 0
        );
        MenuModifierGroupItem group = new MenuModifierGroupItem(
            UUID.randomUUID(), "Extras", 0, 3, false, List.of(modifierItem)
        );

        MenuProductItem item = MenuProductItem.from(product, List.of(), List.of(group));

        assertThat(item.modifierGroups()).hasSize(1);
        assertThat(item.modifierGroups().get(0).name()).isEqualTo("Extras");
        assertThat(item.modifierGroups().get(0).modifiers()).hasSize(1);
        assertThat(item.modifierGroups().get(0).modifiers().get(0).name()).isEqualTo("Extra Queso");
    }
}
