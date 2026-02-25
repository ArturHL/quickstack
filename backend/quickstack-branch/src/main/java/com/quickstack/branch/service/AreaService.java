package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.AreaCreateRequest;
import com.quickstack.branch.dto.request.AreaUpdateRequest;
import com.quickstack.branch.dto.response.AreaResponse;
import com.quickstack.branch.entity.Area;
import com.quickstack.branch.repository.AreaRepository;
import com.quickstack.branch.repository.BranchRepository;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for area management operations.
 * <p>
 * Enforces business rules:
 * - Area names must be unique within the same branch
 * - The branch must belong to the requesting tenant
 * - All operations are scoped to a tenant (multi-tenancy enforcement)
 * - Cross-tenant access returns 404 to prevent resource enumeration (ASVS V4.1)
 */
@Service
public class AreaService {

    private static final Logger log = LoggerFactory.getLogger(AreaService.class);

    private final AreaRepository areaRepository;
    private final BranchRepository branchRepository;

    public AreaService(AreaRepository areaRepository, BranchRepository branchRepository) {
        this.areaRepository = areaRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Creates a new area within the specified branch.
     *
     * @param tenantId the tenant that owns the area
     * @param userId   the user creating the area (for audit)
     * @param request  the creation request with area details
     * @return the created area as a response DTO
     * @throws ResourceNotFoundException  if the branch is not found or belongs to another tenant
     * @throws DuplicateResourceException if an area with the same name exists in the branch
     */
    @Transactional
    public AreaResponse createArea(UUID tenantId, UUID userId, AreaCreateRequest request) {
        // Validate branch belongs to tenant — same 404 response for IDOR protection
        validateBranchBelongsToTenant(request.branchId(), tenantId);
        validateAreaNameUniqueness(request.name(), request.branchId(), tenantId, null);

        Area area = new Area();
        area.setTenantId(tenantId);
        area.setBranchId(request.branchId());
        area.setName(request.name());
        area.setDescription(request.description());
        area.setSortOrder(request.effectiveSortOrder());
        area.setCreatedBy(userId);

        Area saved = areaRepository.save(area);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.AREA_CREATED, tenantId, userId, saved.getId(), "AREA");

        return AreaResponse.from(saved);
    }

    /**
     * Updates an existing area.
     * <p>
     * Only non-null fields from the request are applied.
     *
     * @param tenantId the tenant that owns the area
     * @param userId   the user performing the update (for audit)
     * @param areaId   the area to update
     * @param request  the update request with changed fields
     * @return the updated area as a response DTO
     * @throws ResourceNotFoundException  if the area is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name conflicts with an existing area in the branch
     */
    @Transactional
    public AreaResponse updateArea(UUID tenantId, UUID userId, UUID areaId, AreaUpdateRequest request) {
        Area area = findActiveByIdAndTenant(areaId, tenantId);

        if (request.name() != null) {
            validateAreaNameUniqueness(request.name(), area.getBranchId(), tenantId, areaId);
            area.setName(request.name());
        }
        if (request.description() != null) {
            area.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            area.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            area.setActive(request.isActive());
        }
        area.setUpdatedBy(userId);

        Area saved = areaRepository.save(area);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.AREA_UPDATED, tenantId, userId, saved.getId(), "AREA");

        return AreaResponse.from(saved);
    }

    /**
     * Soft-deletes an area.
     *
     * @param tenantId the tenant that owns the area
     * @param userId   the user performing the deletion (for audit)
     * @param areaId   the area to delete
     * @throws ResourceNotFoundException if the area is not found or belongs to another tenant
     */
    @Transactional
    public void deleteArea(UUID tenantId, UUID userId, UUID areaId) {
        Area area = findActiveByIdAndTenant(areaId, tenantId);
        area.softDelete(userId);
        areaRepository.save(area);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.AREA_DELETED, tenantId, userId, areaId, "AREA");
    }

    /**
     * Retrieves an area by ID within the tenant's scope.
     *
     * @param tenantId the tenant that owns the area
     * @param areaId   the area to retrieve
     * @return the area as a response DTO
     * @throws ResourceNotFoundException if the area is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public AreaResponse getArea(UUID tenantId, UUID areaId) {
        Area area = findActiveByIdAndTenant(areaId, tenantId);
        return AreaResponse.from(area);
    }

    /**
     * Lists all non-deleted areas for a specific branch within the tenant's scope.
     *
     * @param tenantId the tenant that owns the areas
     * @param branchId the branch whose areas to list
     * @return list of area response DTOs ordered by sort_order
     * @throws ResourceNotFoundException if the branch is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public List<AreaResponse> listAreasByBranch(UUID tenantId, UUID branchId) {
        validateBranchBelongsToTenant(branchId, tenantId);
        return areaRepository.findAllByBranchIdAndTenantId(branchId, tenantId).stream()
                .map(AreaResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a non-deleted area by ID within the tenant scope.
     * Returns 404 for both missing and cross-tenant resources (IDOR protection).
     */
    private Area findActiveByIdAndTenant(UUID areaId, UUID tenantId) {
        return areaRepository.findByIdAndTenantId(areaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Area", areaId));
    }

    /**
     * Validates that the branch exists and belongs to the requesting tenant.
     * Returns 404 (not 403) for cross-tenant branches to prevent enumeration.
     */
    private void validateBranchBelongsToTenant(UUID branchId, UUID tenantId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    /**
     * Validates that an area name is unique within the branch.
     *
     * @param name      the name to check
     * @param branchId  the branch scope
     * @param tenantId  the tenant scope
     * @param excludeId the area ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if an area with this name already exists
     */
    private void validateAreaNameUniqueness(String name, UUID branchId, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? areaRepository.existsByNameAndBranchIdAndTenantIdAndIdNot(name, branchId, tenantId, excludeId)
                : areaRepository.existsByNameAndBranchIdAndTenantId(name, branchId, tenantId);

        if (exists) {
            log.warn("[BRANCH] ACTION=DUPLICATE_AREA_NAME tenantId={} branchId={} name=\"{}\"",
                    tenantId, branchId, name);
            throw new DuplicateResourceException("Area", "name", name);
        }
    }
}
