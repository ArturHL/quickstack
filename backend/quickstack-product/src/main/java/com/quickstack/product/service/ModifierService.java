package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierUpdateRequest;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import com.quickstack.product.repository.ModifierGroupRepository;
import com.quickstack.product.repository.ModifierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for modifier management (individual options within a modifier group).
 * <p>
 * Business rules:
 * - Modifier must belong to a modifier group in the same tenant
 * - Name must be unique within a modifier group + tenant scope
 * - Only one modifier per group should have isDefault = true
 * - Cannot delete the last active modifier in a group
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body
 * - V4.1: Cross-tenant access returns 404 to prevent resource enumeration (IDOR protection)
 */
@Service
public class ModifierService {

    private static final Logger log = LoggerFactory.getLogger(ModifierService.class);

    private final ModifierRepository modifierRepository;
    private final ModifierGroupRepository modifierGroupRepository;

    public ModifierService(ModifierRepository modifierRepository,
                           ModifierGroupRepository modifierGroupRepository) {
        this.modifierRepository = modifierRepository;
        this.modifierGroupRepository = modifierGroupRepository;
    }

    /**
     * Adds a new modifier to a modifier group.
     * <p>
     * If isDefault = true, resets isDefault = false on all other modifiers in the same group
     * within the same transaction.
     *
     * @param tenantId the tenant that owns the modifier group
     * @param userId   the user creating the modifier (for audit)
     * @param request  the creation request
     * @return the created modifier as a response DTO
     * @throws ResourceNotFoundException  if the modifier group is not found or belongs to another tenant
     * @throws DuplicateResourceException if a modifier with the same name exists in the group
     */
    @Transactional
    public ModifierResponse addModifier(UUID tenantId, UUID userId, ModifierCreateRequest request) {
        findGroupByIdAndTenant(request.modifierGroupId(), tenantId);
        validateModifierNameUniqueness(request.name(), request.modifierGroupId(), tenantId, null);

        if (request.effectiveIsDefault()) {
            resetDefaultModifiers(request.modifierGroupId(), tenantId);
        }

        Modifier modifier = new Modifier();
        modifier.setTenantId(tenantId);
        modifier.setModifierGroupId(request.modifierGroupId());
        modifier.setName(request.name());
        modifier.setPriceAdjustment(request.priceAdjustment());
        modifier.setDefault(request.effectiveIsDefault());
        modifier.setSortOrder(request.effectiveSortOrder());

        Modifier saved = modifierRepository.save(modifier);
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_ADDED, tenantId, userId, saved.getId(), "MODIFIER");

        return ModifierResponse.from(saved);
    }

    /**
     * Updates an existing modifier.
     * <p>
     * If isDefault is set to true, resets isDefault = false on all other modifiers in the same group.
     *
     * @param tenantId   the tenant that owns the modifier
     * @param userId     the user updating the modifier (for audit)
     * @param modifierId the modifier to update
     * @param request    the update request with changed fields
     * @return the updated modifier as a response DTO
     * @throws ResourceNotFoundException  if not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name conflicts with an existing modifier in the group
     */
    @Transactional
    public ModifierResponse updateModifier(
            UUID tenantId, UUID userId, UUID modifierId, ModifierUpdateRequest request) {
        Modifier modifier = findByIdAndTenant(modifierId, tenantId);

        if (request.name() != null) {
            validateModifierNameUniqueness(request.name(), modifier.getModifierGroupId(), tenantId, modifierId);
            modifier.setName(request.name());
        }
        if (request.priceAdjustment() != null) {
            modifier.setPriceAdjustment(request.priceAdjustment());
        }
        if (request.isDefault() != null) {
            if (request.isDefault() && !modifier.isDefault()) {
                resetDefaultModifiers(modifier.getModifierGroupId(), tenantId);
            }
            modifier.setDefault(request.isDefault());
        }
        if (request.isActive() != null) {
            modifier.setActive(request.isActive());
        }
        if (request.sortOrder() != null) {
            modifier.setSortOrder(request.sortOrder());
        }

        Modifier saved = modifierRepository.save(modifier);
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_UPDATED, tenantId, userId, saved.getId(), "MODIFIER");

        return ModifierResponse.from(saved);
    }

    /**
     * Soft-deletes a modifier.
     * <p>
     * Cannot delete the last active modifier in a group — at least one must remain.
     *
     * @param tenantId   the tenant that owns the modifier
     * @param userId     the user deleting the modifier (for audit)
     * @param modifierId the modifier to delete
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     * @throws BusinessRuleException     if it would be the last active modifier in the group
     */
    @Transactional
    public void deleteModifier(UUID tenantId, UUID userId, UUID modifierId) {
        Modifier modifier = findByIdAndTenant(modifierId, tenantId);

        if (modifier.isActive()) {
            long activeCount = modifierRepository.countActiveByModifierGroupId(
                    modifier.getModifierGroupId(), tenantId);
            if (activeCount <= 1) {
                throw new BusinessRuleException("LAST_ACTIVE_MODIFIER",
                        "Cannot delete the last active modifier in a group");
            }
        }

        modifier.setDeletedAt(Instant.now());
        modifierRepository.save(modifier);
        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.MODIFIER_DELETED, tenantId, userId, modifierId, "MODIFIER");
    }

    /**
     * Lists all active, non-deleted modifiers for a modifier group, ordered by sort_order.
     *
     * @param tenantId        the tenant that owns the modifier group
     * @param modifierGroupId the modifier group whose modifiers to list
     * @return list of modifier responses
     */
    @Transactional(readOnly = true)
    public List<ModifierResponse> listModifiers(UUID tenantId, UUID modifierGroupId) {
        return modifierRepository.findAllByModifierGroupIdAndTenantId(modifierGroupId, tenantId)
                .stream()
                .map(ModifierResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ModifierGroup findGroupByIdAndTenant(UUID modifierGroupId, UUID tenantId) {
        return modifierGroupRepository.findByIdAndTenantId(modifierGroupId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup", modifierGroupId));
    }

    private Modifier findByIdAndTenant(UUID modifierId, UUID tenantId) {
        return modifierRepository.findByIdAndTenantId(modifierId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier", modifierId));
    }

    private void validateModifierNameUniqueness(String name, UUID modifierGroupId, UUID tenantId, UUID excludeId) {
        boolean exists = modifierRepository.existsByNameAndModifierGroupIdAndTenantId(name, modifierGroupId, tenantId);
        if (exists) {
            // For updates, check if it's a different modifier
            if (excludeId != null) {
                Modifier existing = modifierRepository.findByIdAndTenantId(excludeId, tenantId).orElse(null);
                if (existing != null && existing.getName().equals(name)) {
                    return; // Same modifier, name unchanged — not a duplicate
                }
            }
            throw new DuplicateResourceException("Modifier", "name", name);
        }
    }

    /**
     * Resets isDefault to false on all currently-default modifiers in the group.
     * Called before setting a new default modifier.
     */
    private void resetDefaultModifiers(UUID modifierGroupId, UUID tenantId) {
        List<Modifier> modifiers = modifierRepository.findAllByModifierGroupIdAndTenantId(
                modifierGroupId, tenantId);
        modifiers.stream()
                .filter(Modifier::isDefault)
                .forEach(m -> m.setDefault(false));
        modifierRepository.saveAll(modifiers);
    }
}
