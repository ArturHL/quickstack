package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.request.VariantUpdateRequest;
import com.quickstack.product.dto.response.VariantResponse;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VariantServiceTest {

    @Mock
    private VariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private VariantService variantService;

    private UUID tenantId;
    private UUID userId;
    private UUID productId;
    private UUID variantId;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        mockProduct = new Product();
        mockProduct.setId(productId);
        mockProduct.setTenantId(tenantId);
        mockProduct.setProductType(ProductType.VARIANT);
        mockProduct.setBasePrice(new BigDecimal("100.00"));
    }

    @Test
    void addVariant_Success() {
        VariantCreateRequest request = new VariantCreateRequest("Chico", "SKU-CH", BigDecimal.ZERO, true, 1);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));
        when(variantRepository.existsByNameAndProductIdAndTenantId("Chico", productId, tenantId)).thenReturn(false);
        when(variantRepository.existsBySkuAndTenantId("SKU-CH", tenantId)).thenReturn(false);

        ProductVariant savedVariant = new ProductVariant();
        savedVariant.setId(variantId);
        savedVariant.setProductId(productId);
        savedVariant.setName("Chico");
        savedVariant.setSku("SKU-CH");
        savedVariant.setPriceAdjustment(BigDecimal.ZERO);
        savedVariant.setDefault(true);
        savedVariant.setSortOrder(1);

        when(variantRepository.save(any(ProductVariant.class))).thenReturn(savedVariant);

        VariantResponse response = variantService.addVariant(tenantId, userId, productId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(variantId);
        assertThat(response.name()).isEqualTo("Chico");
        assertThat(response.effectivePrice()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(variantRepository).save(any(ProductVariant.class));
    }

    @Test
    void addVariant_ProductNotVariant_ThrowsException() {
        mockProduct.setProductType(ProductType.SIMPLE);
        VariantCreateRequest request = new VariantCreateRequest("Chico", "SKU", BigDecimal.ZERO, true, 1);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));

        Throwable thrown = catchThrowable(() -> variantService.addVariant(tenantId, userId, productId, request));

        assertThat(thrown).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Variants can only be managed for products of type VARIANT");
    }

    @Test
    void updateVariant_Success() {
        VariantUpdateRequest request = new VariantUpdateRequest("Medio", null, new BigDecimal("10.00"), null, null,
                null);

        ProductVariant existingVariant = new ProductVariant();
        existingVariant.setId(variantId);
        existingVariant.setName("Chico");
        existingVariant.setSku("SKU-1");

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));
        when(variantRepository.findByIdAndProductIdAndTenantId(variantId, productId, tenantId))
                .thenReturn(Optional.of(existingVariant));
        when(variantRepository.existsByNameAndProductIdAndTenantIdAndIdNot("Medio", productId, tenantId, variantId))
                .thenReturn(false);

        when(variantRepository.save(any(ProductVariant.class))).thenReturn(existingVariant);

        VariantResponse response = variantService.updateVariant(tenantId, userId, productId, variantId, request);

        assertThat(response).isNotNull();
        assertThat(existingVariant.getName()).isEqualTo("Medio"); // updated
        assertThat(existingVariant.getSku()).isEqualTo("SKU-1"); // not updated
        assertThat(existingVariant.getPriceAdjustment()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void deleteVariant_Success() {
        ProductVariant existingVariant = new ProductVariant();
        existingVariant.setId(variantId);
        existingVariant.setActive(true);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));
        when(variantRepository.findByIdAndProductIdAndTenantId(variantId, productId, tenantId))
                .thenReturn(Optional.of(existingVariant));
        when(variantRepository.countByProductIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)).thenReturn(2L);

        variantService.deleteVariant(tenantId, userId, productId, variantId);

        assertThat(existingVariant.isDeleted()).isTrue();
        assertThat(existingVariant.isActive()).isFalse();
        verify(variantRepository).save(existingVariant);
    }

    @Test
    void deleteVariant_LastVariant_ThrowsException() {
        ProductVariant existingVariant = new ProductVariant();
        existingVariant.setId(variantId);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));
        when(variantRepository.findByIdAndProductIdAndTenantId(variantId, productId, tenantId))
                .thenReturn(Optional.of(existingVariant));
        when(variantRepository.countByProductIdAndTenantIdAndDeletedAtIsNull(productId, tenantId)).thenReturn(1L);

        Throwable thrown = catchThrowable(() -> variantService.deleteVariant(tenantId, userId, productId, variantId));

        assertThat(thrown).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("A VARIANT product must have at least one variant");
    }

    @Test
    void listVariants_Success() {
        ProductVariant v1 = new ProductVariant();
        v1.setName("Chico");
        v1.setPriceAdjustment(BigDecimal.ZERO);

        ProductVariant v2 = new ProductVariant();
        v2.setName("Grande");
        v2.setPriceAdjustment(new BigDecimal("20.00"));

        List<ProductVariant> variants = List.of(v1, v2);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(mockProduct));
        when(variantRepository.findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(productId, tenantId))
                .thenReturn(variants);

        List<VariantResponse> responses = variantService.listVariants(tenantId, productId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).effectivePrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(responses.get(1).effectivePrice()).isEqualByComparingTo(new BigDecimal("120.00"));
    }
}
