package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.request.VariantUpdateRequest;
import com.quickstack.product.dto.response.VariantResponse;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing product variants.
 */
@Service
public class VariantService {

    private static final Logger log = LoggerFactory.getLogger(VariantService.class);

    private final VariantRepository variantRepository;
    private final ProductRepository productRepository;

    public VariantService(VariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public VariantResponse addVariant(UUID tenantId, UUID userId, UUID productId, VariantCreateRequest request) {
        Product product = validateProductVariantType(productId, tenantId);

        // Name Uniqueness
        if (variantRepository.existsByNameAndProductIdAndTenantId(request.name(), productId, tenantId)) {
            throw new DuplicateResourceException("ProductVariant", "name", request.name());
        }

        // SKU Uniqueness
        if (request.sku() != null && !request.sku().isBlank()) {
            if (variantRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
                throw new DuplicateResourceException("ProductVariant", "sku", request.sku());
            }
        }

        ProductVariant variant = new ProductVariant();
        variant.setTenantId(tenantId);
        variant.setProductId(productId);
        variant.setName(request.name());
        variant.setSku(request.sku());
        variant.setPriceAdjustment(request.priceAdjustment());
        variant.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        variant.setDefault(request.isDefault());

        if (variant.isDefault()) {
            resetOtherDefaults(productId, tenantId, null);
        }

        variant = variantRepository.save(variant);

        // Ensure at least one default remains if needed
        ensureDefaultVariant(productId, tenantId);
        
        logInfo(CatalogAction.VARIANT_ADDED, tenantId, userId, variant.getId(), "PRODUCT_VARIANT");

        return VariantResponse.from(variant, product.getBasePrice());
    }

    @Transactional
    public VariantResponse updateVariant(UUID tenantId, UUID userId, UUID productId, UUID variantId, VariantUpdateRequest request) {
        Product product = validateProductVariantType(productId, tenantId);

        ProductVariant variant = variantRepository.findByIdAndProductIdAndTenantId(variantId, productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));

        // Name uniqueness excluding self
        if (request.name() != null && !request.name().equals(variant.getName())) {
            if (variantRepository.existsByNameAndProductIdAndTenantIdAndIdNot(request.name(), productId, tenantId, variantId)) {
                 throw new DuplicateResourceException("ProductVariant", "name", request.name());
            }
            variant.setName(request.name());
        }

        // SKU uniqueness excluding self
        if (request.sku() != null && !request.sku().equals(variant.getSku())) {
            if (variantRepository.existsBySkuAndTenantIdAndIdNot(request.sku(), tenantId, variantId)) {
                 throw new DuplicateResourceException("ProductVariant", "sku", request.sku());
            }
            variant.setSku(request.sku());
        }

        if (request.priceAdjustment() != null) variant.setPriceAdjustment(request.priceAdjustment());
        if (request.sortOrder() != null) variant.setSortOrder(request.sortOrder());
        if (request.isActive() != null) variant.setActive(request.isActive());

        if (request.isDefault() != null) {
            if (request.isDefault()) {
                resetOtherDefaults(productId, tenantId, variantId);
                variant.setDefault(true);
            } else if (variant.isDefault() && !request.isDefault()) {
                 // Trying to unset the only default
                 variant.setDefault(false);
            }
        }

        variant = variantRepository.save(variant);
        
        // Safety net: exactly one must be default
        ensureDefaultVariant(productId, tenantId);

        logInfo(CatalogAction.VARIANT_UPDATED, tenantId, userId, variant.getId(), "PRODUCT_VARIANT");
        
        return VariantResponse.from(variant, product.getBasePrice());
    }

    @Transactional
    public void deleteVariant(UUID tenantId, UUID userId, UUID productId, UUID variantId) {
        validateProductVariantType(productId, tenantId);

        ProductVariant variant = variantRepository.findByIdAndProductIdAndTenantId(variantId, productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));

        long count = variantRepository.countByProductIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);

        if (count <= 1) {
             throw new BusinessRuleException("MIN_VARIANTS_REACHED", "A VARIANT product must have at least one variant");
        }

        variant.softDelete();
        variantRepository.save(variant);

        ensureDefaultVariant(productId, tenantId);
        
        logInfo(CatalogAction.VARIANT_DELETED, tenantId, userId, variantId, "PRODUCT_VARIANT");
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listVariants(UUID tenantId, UUID productId) {
        Product product = validateProductVariantType(productId, tenantId);

        List<ProductVariant> variants = variantRepository.findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(productId, tenantId);

        return variants.stream()
                .map(v -> VariantResponse.from(v, product.getBasePrice()))
                .collect(Collectors.toList());
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private Product validateProductVariantType(UUID productId, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getProductType() != ProductType.VARIANT) {
            throw new BusinessRuleException("PRODUCT_NOT_VARIANT", "Variants can only be managed for products of type VARIANT");
        }
        return product;
    }

    private void resetOtherDefaults(UUID productId, UUID tenantId, UUID excludeVariantId) {
         List<ProductVariant> allActive = variantRepository.findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(productId, tenantId);
         for (ProductVariant v : allActive) {
             if (v.isDefault() && (excludeVariantId == null || !v.getId().equals(excludeVariantId))) {
                 v.setDefault(false);
                 variantRepository.save(v);
             }
         }
    }

    private void ensureDefaultVariant(UUID productId, UUID tenantId) {
        List<ProductVariant> allActive = variantRepository.findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(productId, tenantId);
        
        if (allActive.isEmpty()) return;

        boolean hasDefault = allActive.stream().anyMatch(ProductVariant::isDefault);

        if (!hasDefault) {
            ProductVariant first = allActive.get(0);
            first.setDefault(true);
            variantRepository.save(first);
        }
    }

    private void logInfo(CatalogAction action, UUID tenantId, UUID userId, UUID resourceId, String resourceType) {
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                action, tenantId, userId, resourceId, resourceType);
    }
}
