package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.BranchCreateRequest;
import com.quickstack.branch.dto.request.BranchUpdateRequest;
import com.quickstack.branch.dto.response.BranchResponse;
import com.quickstack.branch.entity.Branch;
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
 * Service for branch management operations.
 * <p>
 * Enforces business rules:
 * - Branch names must be unique within the same tenant
 * - Branch codes must be unique within the same tenant
 * - All operations are scoped to a tenant (multi-tenancy enforcement)
 * - Cross-tenant access returns 404 to prevent resource enumeration (ASVS V4.1)
 */
@Service
public class BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchService.class);

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /**
     * Creates a new branch for the given tenant.
     *
     * @param tenantId the tenant that owns the branch
     * @param userId   the user creating the branch (for audit)
     * @param request  the creation request with branch details
     * @return the created branch as a response DTO
     * @throws DuplicateResourceException if a branch with the same name or code exists
     */
    @Transactional
    public BranchResponse createBranch(UUID tenantId, UUID userId, BranchCreateRequest request) {
        validateNameUniqueness(request.name(), tenantId, null);
        validateCodeUniqueness(request.code(), tenantId, null);

        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setName(request.name());
        branch.setCode(request.code());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setPhone(request.phone());
        branch.setEmail(request.email());
        branch.setCreatedBy(userId);

        Branch saved = branchRepository.save(branch);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.BRANCH_CREATED, tenantId, userId, saved.getId(), "BRANCH");

        return BranchResponse.from(saved);
    }

    /**
     * Updates an existing branch.
     * <p>
     * Only non-null fields from the request are applied.
     *
     * @param tenantId the tenant that owns the branch
     * @param userId   the user performing the update (for audit)
     * @param branchId the branch to update
     * @param request  the update request with changed fields
     * @return the updated branch as a response DTO
     * @throws ResourceNotFoundException  if the branch is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name or code conflicts with an existing branch
     */
    @Transactional
    public BranchResponse updateBranch(UUID tenantId, UUID userId, UUID branchId, BranchUpdateRequest request) {
        Branch branch = findActiveByIdAndTenant(branchId, tenantId);

        if (request.name() != null) {
            validateNameUniqueness(request.name(), tenantId, branchId);
            branch.setName(request.name());
        }
        if (request.code() != null) {
            validateCodeUniqueness(request.code(), tenantId, branchId);
            branch.setCode(request.code());
        }
        if (request.address() != null) {
            branch.setAddress(request.address());
        }
        if (request.city() != null) {
            branch.setCity(request.city());
        }
        if (request.phone() != null) {
            branch.setPhone(request.phone());
        }
        if (request.email() != null) {
            branch.setEmail(request.email());
        }
        if (request.isActive() != null) {
            branch.setActive(request.isActive());
        }
        branch.setUpdatedBy(userId);

        Branch saved = branchRepository.save(branch);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.BRANCH_UPDATED, tenantId, userId, saved.getId(), "BRANCH");

        return BranchResponse.from(saved);
    }

    /**
     * Soft-deletes a branch.
     *
     * @param tenantId the tenant that owns the branch
     * @param userId   the user performing the deletion (for audit)
     * @param branchId the branch to delete
     * @throws ResourceNotFoundException if the branch is not found or belongs to another tenant
     */
    @Transactional
    public void deleteBranch(UUID tenantId, UUID userId, UUID branchId) {
        Branch branch = findActiveByIdAndTenant(branchId, tenantId);
        branch.softDelete(userId);
        branchRepository.save(branch);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.BRANCH_DELETED, tenantId, userId, branchId, "BRANCH");
    }

    /**
     * Retrieves a branch by ID within the tenant's scope.
     *
     * @param tenantId the tenant that owns the branch
     * @param branchId the branch to retrieve
     * @return the branch as a response DTO
     * @throws ResourceNotFoundException if the branch is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public BranchResponse getBranch(UUID tenantId, UUID branchId) {
        Branch branch = findActiveByIdAndTenant(branchId, tenantId);
        return BranchResponse.from(branch);
    }

    /**
     * Lists all non-deleted branches for a tenant.
     *
     * @param tenantId the tenant whose branches to list
     * @return list of branch response DTOs
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(UUID tenantId) {
        return branchRepository.findAllByTenantId(tenantId).stream()
                .map(BranchResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a non-deleted branch by ID within the tenant scope.
     * Returns 404 for both missing and cross-tenant resources (IDOR protection).
     */
    private Branch findActiveByIdAndTenant(UUID branchId, UUID tenantId) {
        return branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
    }

    /**
     * Validates that a branch name is unique within the tenant.
     *
     * @param name      the name to check
     * @param tenantId  the tenant scope
     * @param excludeId the branch ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if a branch with this name already exists
     */
    private void validateNameUniqueness(String name, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? branchRepository.existsByNameAndTenantIdAndIdNot(name, tenantId, excludeId)
                : branchRepository.existsByNameAndTenantId(name, tenantId);

        if (exists) {
            log.warn("[BRANCH] ACTION=DUPLICATE_NAME tenantId={} name=\"{}\"", tenantId, name);
            throw new DuplicateResourceException("Branch", "name", name);
        }
    }

    /**
     * Validates that a branch code is unique within the tenant.
     *
     * @param code      the code to check
     * @param tenantId  the tenant scope
     * @param excludeId the branch ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if a branch with this code already exists
     */
    private void validateCodeUniqueness(String code, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? branchRepository.existsByCodeAndTenantIdAndIdNot(code, tenantId, excludeId)
                : branchRepository.existsByCodeAndTenantId(code, tenantId);

        if (exists) {
            log.warn("[BRANCH] ACTION=DUPLICATE_CODE tenantId={} code=\"{}\"", tenantId, code);
            throw new DuplicateResourceException("Branch", "code", code);
        }
    }
}
