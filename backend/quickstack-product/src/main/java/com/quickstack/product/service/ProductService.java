package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ProductAvailabilityRequest;
import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.dto.request.ProductUpdateRequest;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.response.CategorySummaryResponse;
import com.quickstack.product.dto.response.ProductResponse;
import com.quickstack.product.dto.response.ProductSummaryResponse;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import com.quickstack.product.repository.CategoryRepository;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for product management including creation, updates, and availability.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VariantRepository variantRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, VariantRepository variantRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
    }

    /**
     * Creates a new product for the tenant.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID performing the creation
     * @param request the creation request
     * @return the created product details
     */
    @Transactional
    public ProductResponse createProduct(UUID tenantId, UUID userId, ProductCreateRequest request) {
        // 1. Validate Category
        Category category = categoryRepository.findByIdAndTenantId(request.categoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        // 2. Validate SKU Uniqueness
        if (request.sku() != null && !request.sku().isBlank()) {
            if (productRepository.existsBySkuAndTenantId(request.sku(), tenantId)) {
                String reason = String.format("Product with sku %s already exists", request.sku());
                logWarn(CatalogAction.PRODUCT_CREATED, tenantId, userId, null, "PRODUCT", reason);
                throw new DuplicateResourceException("Product", "sku", request.sku());
            }
        }

        // 3. Validate Name Uniqueness within Category
        if (productRepository.existsByNameAndTenantIdAndCategoryId(request.name(), tenantId, request.categoryId())) {
            String reason = String.format("Product with name %s already exists in category", request.name());
            logWarn(CatalogAction.PRODUCT_CREATED, tenantId, userId, null, "PRODUCT", reason);
            throw new DuplicateResourceException("Product", "name", request.name());
        }

        // 4. Create Product (Initial save to generate ID)
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCreatedBy(userId);
        updateProductFromRequest(product, request);

        product = productRepository.save(product);

        // 5. Handle Variants if VARIANT type
        if (product.getProductType() == ProductType.VARIANT) {
            handleNewVariants(product, request.variants(), tenantId);
        }

        logInfo(CatalogAction.PRODUCT_CREATED, tenantId, userId, product.getId(), "PRODUCT");
        return ProductResponse.from(product, CategorySummaryResponse.from(category));
    }

    /**
     * Updates an existing product.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID performing the update
     * @param productId the product ID to update
     * @param request the update request
     * @return the updated product details
     */
    @Transactional
    public ProductResponse updateProduct(UUID tenantId, UUID userId, UUID productId, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        // 1. Validate Category change
        if (request.categoryId() != null && !request.categoryId().equals(product.getCategoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) { // Should be tenant-safe check
                 throw new ResourceNotFoundException("Category", request.categoryId());
            }
            product.setCategoryId(request.categoryId());
        }

        // 2. Validate SKU Uniqueness if changed
        if (request.sku() != null && !request.sku().equals(product.getSku())) {
            if (productRepository.existsBySkuAndTenantIdAndIdNot(request.sku(), tenantId, productId)) {
                String reason = String.format("Product with sku %s already exists", request.sku());
                logWarn(CatalogAction.PRODUCT_UPDATED, tenantId, userId, productId, "PRODUCT", reason);
                throw new DuplicateResourceException("Product", "sku", request.sku());
            }
            product.setSku(request.sku());
        }

        // 3. Validate Name Uniqueness within Category if changed
        UUID effectiveCategoryId = request.categoryId() != null ? request.categoryId() : product.getCategoryId();
        String effectiveName = request.name() != null ? request.name() : product.getName();
        if (productRepository.existsByNameAndTenantIdAndCategoryIdAndIdNot(effectiveName, tenantId, effectiveCategoryId, productId)) {
            String reason = String.format("Product with name %s already exists in category", effectiveName);
            logWarn(CatalogAction.PRODUCT_UPDATED, tenantId, userId, productId, "PRODUCT", reason);
            throw new DuplicateResourceException("Product", "name", effectiveName);
        }

        // 4. Update core fields
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.basePrice() != null) product.setBasePrice(request.basePrice());
        if (request.costPrice() != null) product.setCostPrice(request.costPrice());
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.productType() != null) {
            // Business Rule: Changing product type is complex, but required for this sprint
            if (product.getProductType() != request.productType()) {
                 product.setProductType(request.productType());
            }
        }
        if (request.sortOrder() != null) product.setSortOrder(request.sortOrder());
        if (request.isActive() != null) product.setActive(request.isActive());

        // 5. Handle variants if type is VARIANT or changed to VARIANT
        if (product.getProductType() == ProductType.VARIANT && request.variants() != null) {
            product.getVariants().clear(); // Roadmap says replace all variants
            handleNewVariants(product, request.variants(), tenantId);
        } else if (product.getProductType() != ProductType.VARIANT) {
            product.getVariants().clear();
        }

        product.setUpdatedBy(userId);
        product = productRepository.save(product);

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        CategorySummaryResponse categorySummary = category != null ? CategorySummaryResponse.from(category) : null;

        logInfo(CatalogAction.PRODUCT_UPDATED, tenantId, userId, product.getId(), "PRODUCT");
        return ProductResponse.from(product, categorySummary);
    }

    /**
     * Reorders a list of products for the given tenant.
     *
     * @param tenantId the tenant that owns the products
     * @param userId   the user performing the operation
     * @param items    the list of items to reorder
     */
    @Transactional
    public void reorderProducts(UUID tenantId, UUID userId, List<com.quickstack.product.dto.request.ReorderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<UUID> itemIds = items.stream().map(com.quickstack.product.dto.request.ReorderItem::id).collect(Collectors.toSet());
        List<Product> products = productRepository.findByIdInAndTenantId(itemIds, tenantId);

        if (products.size() != itemIds.size()) {
            String reason = "One or more products not found or belong to another tenant";
            logWarn(CatalogAction.PRODUCT_REORDERED, tenantId, userId, null, "PRODUCT", reason);
            throw new BusinessRuleException("INVALID_PRODUCTS", reason);
        }

        Map<UUID, Integer> orderMap = items.stream()
            .collect(Collectors.toMap(com.quickstack.product.dto.request.ReorderItem::id, com.quickstack.product.dto.request.ReorderItem::sortOrder));

        for (Product product : products) {
            Integer newSortOrder = orderMap.get(product.getId());
            if (newSortOrder != null && !newSortOrder.equals(product.getSortOrder())) {
                product.setSortOrder(newSortOrder);
                product.setUpdatedBy(userId);
                logInfo(CatalogAction.PRODUCT_REORDERED, tenantId, userId, product.getId(), "PRODUCT");
            }
        }
        productRepository.saveAll(products);
    }

    /**
     * Gets a single product by ID.
     *
     * @param tenantId the tenant ID
     * @param productId the product ID
     * @return the product details
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID tenantId, UUID productId) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        CategorySummaryResponse categorySummary = category != null ? CategorySummaryResponse.from(category) : null;

        return ProductResponse.from(product, categorySummary);
    }

    /**
     * Lists products with filtering and pagination.
     *
     * @param tenantId the tenant ID
     * @param categoryId optional category filter
     * @param isAvailable optional availability filter
     * @param nameSearch optional name search
     * @param includeInactive if true, includes inactive products (OWNER/MANAGER)
     * @param pageable pagination parameters
     * @return page of product summaries
     */
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listProducts(
            UUID tenantId,
            UUID categoryId,
            Boolean isAvailable,
            String nameSearch,
            boolean includeInactive,
            Pageable pageable) {

        Boolean isActive = includeInactive ? null : true;
        Page<Product> products = productRepository.findAllByTenantIdWithFilters(
            tenantId, categoryId, isActive, isAvailable, nameSearch, pageable);
        
        return products.map(ProductSummaryResponse::from);
    }

    /**
     * Soft deletes a product.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID performing the deletion
     * @param productId the product ID to delete
     */
    @Transactional
    public void deleteProduct(UUID tenantId, UUID userId, UUID productId) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.softDelete(userId);
        productRepository.save(product);

        logInfo(CatalogAction.PRODUCT_DELETED, tenantId, userId, productId, "PRODUCT");
    }

    /**
     * Changes product availability.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID performing the action
     * @param productId the product ID
     * @param isAvailable the new availability status
     * @return the updated product details
     */
    @Transactional
    public ProductResponse setAvailability(UUID tenantId, UUID userId, UUID productId, boolean isAvailable) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.setAvailable(isAvailable);
        product = productRepository.save(product);

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        CategorySummaryResponse categorySummary = category != null ? CategorySummaryResponse.from(category) : null;

        logInfo(CatalogAction.PRODUCT_AVAILABILITY_CHANGED, tenantId, userId, productId, "PRODUCT");
        return ProductResponse.from(product, categorySummary);
    }

    /**
     * Restores a soft-deleted product.
     *
     * @param tenantId the tenant ID
     * @param userId the user ID performing the restoration
     * @param productId the product ID to restore
     * @return the restored product details
     */
    @Transactional
    public ProductResponse restoreProduct(UUID tenantId, UUID userId, UUID productId) {
        Product product = productRepository.findByIdAndTenantIdIncludingDeleted(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (!product.isDeleted()) {
             // Already active, nothing to do
             return getProduct(tenantId, productId);
        }

        product.restore();
        product.setUpdatedBy(userId);
        product = productRepository.save(product);

        Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        CategorySummaryResponse categorySummary = category != null ? CategorySummaryResponse.from(category) : null;

        logInfo(CatalogAction.PRODUCT_RESTORED, tenantId, userId, productId, "PRODUCT");
        return ProductResponse.from(product, categorySummary);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private void updateProductFromRequest(Product product, ProductCreateRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategoryId(request.categoryId());
        product.setSku(request.sku());
        product.setBasePrice(request.basePrice());
        product.setCostPrice(request.costPrice());
        product.setImageUrl(request.imageUrl());
        product.setProductType(request.productType());
        product.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    }

    private void handleNewVariants(Product product, List<VariantCreateRequest> variantRequests, UUID tenantId) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            String reason = "Product of type VARIANT must have at least one variant";
            logWarn(CatalogAction.PRODUCT_CREATED, tenantId, null, null, "PRODUCT", reason);
            throw new BusinessRuleException("VARIANT_PRODUCT_REQUIRES_VARIANTS", reason);
        }

        boolean hasDefault = false;
        for (VariantCreateRequest vr : variantRequests) {
            ProductVariant variant = new ProductVariant();
            variant.setTenantId(tenantId);
            variant.setProductId(product.getId()); // Might be null during create, handled by cascade save
            variant.setName(vr.name());
            variant.setSku(vr.sku());
            variant.setPriceAdjustment(vr.priceAdjustment());
            variant.setSortOrder(vr.sortOrder() != null ? vr.sortOrder() : 0);
            
            if (vr.isDefault()) {
                if (hasDefault) {
                    // Only one can be default, reset previous if needed or throw error
                    // Strategy: last one wins or throw? Roadmap says "exactly one", I'll mark previous as false
                    product.getVariants().forEach(v -> v.setDefault(false));
                }
                variant.setDefault(true);
                hasDefault = true;
            }
            
            variant = variantRepository.save(variant);
            product.getVariants().add(variant);
        }

        // If no default was specified, make the first one default
        if (!hasDefault && !product.getVariants().isEmpty()) {
            ProductVariant first = product.getVariants().get(0);
            first.setDefault(true);
            variantRepository.save(first);
        }
    }

    private void logInfo(CatalogAction action, UUID tenantId, UUID userId, UUID resourceId, String resourceType) {
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                action, tenantId, userId, resourceId != null ? resourceId : "BATCH", resourceType);
    }

    private void logWarn(CatalogAction action, UUID tenantId, UUID userId, UUID resourceId, String resourceType, String reason) {
        log.warn("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={} reason=\"{}\"",
                action, tenantId, userId != null ? userId : "SYSTEM", resourceId != null ? resourceId : "BATCH", resourceType, reason);
    }
}
