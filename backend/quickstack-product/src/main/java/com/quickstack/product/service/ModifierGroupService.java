package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import com.quickstack.product.dto.response.ModifierGroupResponse;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import com.quickstack.product.repository.ModifierGroupRepository;
import com.quickstack.product.repository.ModifierRepository;
import com.quickstack.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for modifier group management.
 * <p>
 * Business rules:
 * - Modifier group must belong to a product in the same tenant
 * - Name must be unique within a product + tenant scope
 * - If isRequired = true, minSelections must be >= 1
 * - maxSelections must be >= minSelections when both are set
 * - Soft-deleting a group cascades to all its modifiers
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body
 * - V4.1: Cross-tenant access returns 404 to prevent resource enumeration (IDOR protection)
 */
@Service
public class ModifierGroupService {

    private static final Logger log = LoggerFactory.getLogger(ModifierGroupService.class);

    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final ProductRepository productRepository;

    public ModifierGroupService(
            ModifierGroupRepository modifierGroupRepository,
            ModifierRepository modifierRepository,
            ProductRepository productRepository) {
        this.modifierGroupRepository = modifierGroupRepository;
        this.modifierRepository = modifierRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a new modifier group for a product.
     *
     * @param tenantId the tenant that owns the product
     * @param userId   the user creating the group (for audit)
     * @param request  the creation request
     * @return the created modifier group as a response DTO
     * @throws ResourceNotFoundException  if the product is not found or belongs to another tenant
     * @throws DuplicateResourceException if a modifier group with the same name exists for the product
     * @throws BusinessRuleException      if isRequired=true but minSelections=0
     */
    @Transactional
    public ModifierGroupResponse createModifierGroup(UUID tenantId, UUID userId, ModifierGroupCreateRequest request) {
        validateProductBelongsToTenant(request.productId(), tenantId);
        validateRequiredConfig(request.isRequired(), request.minSelections(), request.maxSelections());
        validateGroupNameUniqueness(request.name(), request.productId(), tenantId, null);

        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantId);
        group.setProductId(request.productId());
        group.setName(request.name());
        group.setDescription(request.description());
        group.setMinSelections(request.minSelections());
        group.setMaxSelections(request.maxSelections());
        group.setRequired(request.isRequired());
        group.setSortOrder(request.effectiveSortOrder());

        ModifierGroup saved = modifierGroupRepository.save(group);
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_GROUP_CREATED, tenantId, userId, saved.getId(), "MODIFIER_GROUP");

        return ModifierGroupResponse.from(saved, List.of());
    }

    /**
     * Updates an existing modifier group.
     *
     * @param tenantId        the tenant that owns the modifier group
     * @param userId          the user updating the group (for audit)
     * @param modifierGroupId the modifier group to update
     * @param request         the update request with changed fields
     * @return the updated modifier group as a response DTO
     * @throws ResourceNotFoundException  if not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name conflicts with an existing group
     * @throws BusinessRuleException      if the resulting required/min config is invalid
     */
    @Transactional
    public ModifierGroupResponse updateModifierGroup(
            UUID tenantId, UUID userId, UUID modifierGroupId, ModifierGroupUpdateRequest request) {
        ModifierGroup group = findByIdAndTenant(modifierGroupId, tenantId);

        if (request.name() != null) {
            validateGroupNameUniqueness(request.name(), group.getProductId(), tenantId, modifierGroupId);
            group.setName(request.name());
        }
        if (request.description() != null) {
            group.setDescription(request.description());
        }
        if (request.minSelections() != null) {
            group.setMinSelections(request.minSelections());
        }
        if (request.maxSelections() != null) {
            group.setMaxSelections(request.maxSelections());
        }
        if (request.isRequired() != null) {
            group.setRequired(request.isRequired());
        }
        if (request.sortOrder() != null) {
            group.setSortOrder(request.sortOrder());
        }

        // Validate effective state after partial update
        validateRequiredConfig(group.isRequired(), group.getMinSelections(), group.getMaxSelections());

        ModifierGroup saved = modifierGroupRepository.save(group);
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_GROUP_UPDATED, tenantId, userId, saved.getId(), "MODIFIER_GROUP");

        List<ModifierResponse> modifiers = loadModifiers(modifierGroupId, tenantId);
        return ModifierGroupResponse.from(saved, modifiers);
    }

    /**
     * Soft-deletes a modifier group and cascades the soft delete to all its modifiers.
     *
     * @param tenantId        the tenant that owns the modifier group
     * @param userId          the user deleting the group (for audit)
     * @param modifierGroupId the modifier group to delete
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     */
    @Transactional
    public void deleteModifierGroup(UUID tenantId, UUID userId, UUID modifierGroupId) {
        ModifierGroup group = findByIdAndTenant(modifierGroupId, tenantId);

        Instant now = Instant.now();
        group.setDeletedAt(now);
        modifierGroupRepository.save(group);

        // Cascade soft delete to all non-deleted modifiers
        List<Modifier> modifiers = modifierRepository.findAllNonDeletedByModifierGroupIdAndTenantId(
                modifierGroupId, tenantId);
        modifiers.forEach(m -> m.setDeletedAt(now));
        modifierRepository.saveAll(modifiers);

        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_GROUP_DELETED, tenantId, userId, modifierGroupId, "MODIFIER_GROUP");
    }

    /**
     * Retrieves a modifier group with its active modifiers.
     *
     * @param tenantId        the tenant that owns the modifier group
     * @param modifierGroupId the modifier group to retrieve
     * @return the modifier group response with its modifiers
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public ModifierGroupResponse getModifierGroup(UUID tenantId, UUID modifierGroupId) {
        ModifierGroup group = findByIdAndTenant(modifierGroupId, tenantId);
        List<ModifierResponse> modifiers = loadModifiers(modifierGroupId, tenantId);
        return ModifierGroupResponse.from(group, modifiers);
    }

    /**
     * Lists all modifier groups for a product, ordered by sort_order.
     *
     * @param tenantId  the tenant that owns the product
     * @param productId the product whose modifier groups to list
     * @return list of modifier group responses (each with their modifiers)
     */
    @Transactional(readOnly = true)
    public List<ModifierGroupResponse> listModifierGroupsByProduct(UUID tenantId, UUID productId) {
        List<ModifierGroup> groups = modifierGroupRepository.findAllByProductIdAndTenantId(productId, tenantId);
        return groups.stream()
                .map(g -> ModifierGroupResponse.from(g, loadModifiers(g.getId(), tenantId)))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ModifierGroup findByIdAndTenant(UUID modifierGroupId, UUID tenantId) {
        return modifierGroupRepository.findByIdAndTenantId(modifierGroupId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup", modifierGroupId));
    }

    private void validateProductBelongsToTenant(UUID productId, UUID tenantId) {
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private void validateGroupNameUniqueness(String name, UUID productId, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? modifierGroupRepository.existsByNameAndProductIdAndTenantIdAndIdNot(name, productId, tenantId, excludeId)
                : modifierGroupRepository.existsByNameAndProductIdAndTenantId(name, productId, tenantId);

        if (exists) {
            throw new DuplicateResourceException("ModifierGroup", "name", name);
        }
    }

    private void validateRequiredConfig(boolean isRequired, int minSelections, Integer maxSelections) {
        if (isRequired && minSelections < 1) {
            throw new BusinessRuleException("INVALID_REQUIRED_CONFIG",
                    "If isRequired is true, minSelections must be >= 1");
        }
        if (maxSelections != null && maxSelections < minSelections) {
            throw new BusinessRuleException("INVALID_SELECTION_RANGE",
                    "maxSelections must be >= minSelections");
        }
    }

    private List<ModifierResponse> loadModifiers(UUID modifierGroupId, UUID tenantId) {
        return modifierRepository.findAllByModifierGroupIdAndTenantId(modifierGroupId, tenantId)
                .stream()
                .map(ModifierResponse::from)
                .toList();
    }
}
