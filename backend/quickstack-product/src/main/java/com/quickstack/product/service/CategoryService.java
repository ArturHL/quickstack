package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
import com.quickstack.product.dto.response.CategoryResponse;
import com.quickstack.product.entity.Category;
import com.quickstack.product.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for category management operations.
 * <p>
 * Enforces business rules:
 * - Category names must be unique within the same tenant and parent level
 * - Categories with active products cannot be deleted
 * - All operations are scoped to a tenant (multi-tenancy enforcement)
 * - Cross-tenant access returns 404 to prevent resource enumeration (ASVS V4.1)
 */
@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a new category for the given tenant.
     *
     * @param tenantId the tenant that owns the category
     * @param userId   the user creating the category (for audit)
     * @param request  the creation request with category details
     * @return the created category as a response DTO
     * @throws DuplicateResourceException if a category with the same name exists at the same parent level
     */
    @Transactional
    public CategoryResponse createCategory(UUID tenantId, UUID userId, CategoryCreateRequest request) {
        validateNameUniqueness(request.name(), tenantId, request.parentId(), null);

        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setParentId(request.parentId());
        category.setSortOrder(request.effectiveSortOrder());
        category.setCreatedBy(userId);

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, tenant={}, name={}", saved.getId(), tenantId, saved.getName());

        return toResponse(saved);
    }

    /**
     * Updates an existing category.
     * <p>
     * Only non-null fields from the request are applied.
     *
     * @param tenantId   the tenant that owns the category
     * @param userId     the user performing the update (for audit)
     * @param categoryId the category to update
     * @param request    the update request with changed fields
     * @return the updated category as a response DTO
     * @throws ResourceNotFoundException  if the category is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name conflicts with an existing category
     */
    @Transactional
    public CategoryResponse updateCategory(UUID tenantId, UUID userId, UUID categoryId,
                                           CategoryUpdateRequest request) {
        Category category = findActiveByIdAndTenant(categoryId, tenantId);

        if (request.name() != null) {
            validateNameUniqueness(request.name(), tenantId, category.getParentId(), categoryId);
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            category.setImageUrl(request.imageUrl());
        }
        if (request.parentId() != null) {
            category.setParentId(request.parentId());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            category.setActive(request.isActive());
        }
        category.setUpdatedBy(userId);

        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}, tenant={}", categoryId, tenantId);

        return toResponse(saved);
    }

    /**
     * Soft-deletes a category.
     * <p>
     * The category is not removed from the database — instead it is marked
     * as deleted with a timestamp and the deleting user's ID.
     *
     * @param tenantId   the tenant that owns the category
     * @param userId     the user performing the deletion (for audit)
     * @param categoryId the category to delete
     * @throws ResourceNotFoundException if the category is not found or belongs to another tenant
     * @throws BusinessRuleException     if the category has active products
     */
    @Transactional
    public void deleteCategory(UUID tenantId, UUID userId, UUID categoryId) {
        Category category = findActiveByIdAndTenant(categoryId, tenantId);

        long activeProductCount = categoryRepository.countActiveProductsByCategory(categoryId, tenantId);
        if (activeProductCount > 0) {
            throw new BusinessRuleException(
                "CATEGORY_HAS_PRODUCTS",
                String.format("Cannot delete category: it has %d active product(s)", activeProductCount)
            );
        }

        category.softDelete(userId);
        categoryRepository.save(category);
        log.info("Category soft-deleted: id={}, tenant={}, by={}", categoryId, tenantId, userId);
    }

    /**
     * Retrieves a category by ID within the tenant's scope.
     * <p>
     * Includes inactive categories but excludes soft-deleted ones.
     *
     * @param tenantId   the tenant that owns the category
     * @param categoryId the category to retrieve
     * @return the category as a response DTO
     * @throws ResourceNotFoundException if the category is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID tenantId, UUID categoryId) {
        Category category = findActiveByIdAndTenant(categoryId, tenantId);
        return toResponse(category);
    }

    /**
     * Restores a previously soft-deleted category.
     *
     * @param tenantId   the tenant that owns the category
     * @param userId     the user performing the restore (for audit)
     * @param categoryId the category to restore
     * @return the restored category as a response DTO
     * @throws ResourceNotFoundException if the category is not found or belongs to another tenant
     */
    @Transactional
    public CategoryResponse restoreCategory(UUID tenantId, UUID userId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndTenantIdIncludingDeleted(categoryId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        category.restore();
        category.setUpdatedBy(userId);
        Category saved = categoryRepository.save(category);
        log.info("Category restored: id={}, tenant={}, by={}", categoryId, tenantId, userId);

        return toResponse(saved);
    }

    /**
     * Lists categories for a tenant with optional inactive category inclusion.
     *
     * @param tenantId        the tenant whose categories to list
     * @param includeInactive when true, includes inactive categories (excludes deleted)
     * @param pageable        pagination parameters
     * @return a page of category response DTOs
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> listCategories(UUID tenantId, boolean includeInactive, Pageable pageable) {
        Page<Category> categories = includeInactive
            ? categoryRepository.findAllByTenantIdIncludingInactive(tenantId, pageable)
            : categoryRepository.findAllByTenantId(tenantId, pageable);

        return categories.map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a non-deleted category by ID within the tenant scope.
     * Returns 404 for both missing and cross-tenant resources (IDOR protection).
     */
    private Category findActiveByIdAndTenant(UUID categoryId, UUID tenantId) {
        return categoryRepository.findByIdAndTenantId(categoryId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    /**
     * Validates that a category name is unique within the same tenant and parent level.
     *
     * @param name      the name to check
     * @param tenantId  the tenant scope
     * @param parentId  the parent category level (null = top-level)
     * @param excludeId the category ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if a category with this name already exists
     */
    private void validateNameUniqueness(String name, UUID tenantId, UUID parentId, UUID excludeId) {
        boolean exists = excludeId != null
            ? categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(name, tenantId, parentId, excludeId)
            : categoryRepository.existsByNameAndTenantIdAndParentId(name, tenantId, parentId);

        if (exists) {
            throw new DuplicateResourceException("Category", "name", name);
        }
    }

    /**
     * Converts a Category entity to a CategoryResponse, including the active product count.
     */
    private CategoryResponse toResponse(Category category) {
        long productCount = categoryRepository.countActiveProductsByCategory(
            category.getId(), category.getTenantId());
        return CategoryResponse.from(category, productCount);
    }
}
