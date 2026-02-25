package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.TableCreateRequest;
import com.quickstack.branch.dto.request.TableUpdateRequest;
import com.quickstack.branch.dto.response.TableResponse;
import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.repository.AreaRepository;
import com.quickstack.branch.repository.TableRepository;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for table management operations.
 * <p>
 * Enforces business rules:
 * - Table numbers must be unique within the same area
 * - The area must belong to the requesting tenant
 * - All operations are scoped to a tenant (multi-tenancy enforcement)
 * - Cross-tenant access returns 404 to prevent resource enumeration (ASVS V4.1)
 */
@Service
public class TableService {

    private static final Logger log = LoggerFactory.getLogger(TableService.class);

    private final TableRepository tableRepository;
    private final AreaRepository areaRepository;

    public TableService(TableRepository tableRepository, AreaRepository areaRepository) {
        this.tableRepository = tableRepository;
        this.areaRepository = areaRepository;
    }

    /**
     * Creates a new table within the specified area.
     *
     * @param tenantId the tenant that owns the table
     * @param userId   the user creating the table (for audit)
     * @param request  the creation request with table details
     * @return the created table as a response DTO
     * @throws ResourceNotFoundException  if the area is not found or belongs to another tenant
     * @throws DuplicateResourceException if a table with the same number exists in the area
     */
    @Transactional
    public TableResponse createTable(UUID tenantId, UUID userId, TableCreateRequest request) {
        // Validate area belongs to tenant — same 404 response for IDOR protection
        validateAreaBelongsToTenant(request.areaId(), tenantId);
        validateTableNumberUniqueness(request.number(), request.areaId(), tenantId, null);

        RestaurantTable table = new RestaurantTable();
        table.setTenantId(tenantId);
        table.setAreaId(request.areaId());
        table.setNumber(request.number());
        table.setName(request.name());
        table.setCapacity(request.capacity());
        table.setSortOrder(request.effectiveSortOrder());
        table.setCreatedBy(userId);

        RestaurantTable saved = tableRepository.save(table);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.TABLE_CREATED, tenantId, userId, saved.getId(), "TABLE");

        return TableResponse.from(saved);
    }

    /**
     * Updates an existing table.
     * <p>
     * Only non-null fields from the request are applied.
     * To update status, use {@link #updateTableStatus} instead.
     *
     * @param tenantId the tenant that owns the table
     * @param userId   the user performing the update (for audit)
     * @param tableId  the table to update
     * @param request  the update request with changed fields
     * @return the updated table as a response DTO
     * @throws ResourceNotFoundException  if the table is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new number conflicts with an existing table in the area
     */
    @Transactional
    public TableResponse updateTable(UUID tenantId, UUID userId, UUID tableId, TableUpdateRequest request) {
        RestaurantTable table = findActiveByIdAndTenant(tableId, tenantId);

        if (request.number() != null) {
            validateTableNumberUniqueness(request.number(), table.getAreaId(), tenantId, tableId);
            table.setNumber(request.number());
        }
        if (request.name() != null) {
            table.setName(request.name());
        }
        if (request.capacity() != null) {
            table.setCapacity(request.capacity());
        }
        if (request.sortOrder() != null) {
            table.setSortOrder(request.sortOrder());
        }
        if (request.positionX() != null) {
            table.setPositionX(request.positionX());
        }
        if (request.positionY() != null) {
            table.setPositionY(request.positionY());
        }
        if (request.isActive() != null) {
            table.setActive(request.isActive());
        }
        table.setUpdatedBy(userId);

        RestaurantTable saved = tableRepository.save(table);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.TABLE_UPDATED, tenantId, userId, saved.getId(), "TABLE");

        return TableResponse.from(saved);
    }

    /**
     * Soft-deletes a table.
     *
     * @param tenantId the tenant that owns the table
     * @param userId   the user performing the deletion (for audit)
     * @param tableId  the table to delete
     * @throws ResourceNotFoundException if the table is not found or belongs to another tenant
     */
    @Transactional
    public void deleteTable(UUID tenantId, UUID userId, UUID tableId) {
        RestaurantTable table = findActiveByIdAndTenant(tableId, tenantId);
        table.softDelete(userId);
        tableRepository.save(table);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                BranchAction.TABLE_DELETED, tenantId, userId, tableId, "TABLE");
    }

    /**
     * Retrieves a table by ID within the tenant's scope.
     *
     * @param tenantId the tenant that owns the table
     * @param tableId  the table to retrieve
     * @return the table as a response DTO
     * @throws ResourceNotFoundException if the table is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public TableResponse getTable(UUID tenantId, UUID tableId) {
        RestaurantTable table = findActiveByIdAndTenant(tableId, tenantId);
        return TableResponse.from(table);
    }

    /**
     * Lists all non-deleted tables for a specific area within the tenant's scope.
     *
     * @param tenantId the tenant that owns the tables
     * @param areaId   the area whose tables to list
     * @return list of table response DTOs ordered by sort_order
     * @throws ResourceNotFoundException if the area is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public List<TableResponse> listTablesByArea(UUID tenantId, UUID areaId) {
        validateAreaBelongsToTenant(areaId, tenantId);
        return tableRepository.findAllByAreaIdAndTenantId(areaId, tenantId).stream()
                .map(TableResponse::from)
                .toList();
    }

    /**
     * Updates the status of a table.
     * <p>
     * Status transitions are explicit: AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE.
     *
     * @param tenantId the tenant that owns the table
     * @param tableId  the table to update
     * @param status   the new status
     * @param userId   the user performing the update (for audit)
     * @return the updated table as a response DTO
     * @throws ResourceNotFoundException if the table is not found or belongs to another tenant
     */
    @Transactional
    public TableResponse updateTableStatus(UUID tenantId, UUID tableId, TableStatus status, UUID userId) {
        RestaurantTable table = findActiveByIdAndTenant(tableId, tenantId);
        table.setStatus(status);
        table.setUpdatedBy(userId);

        RestaurantTable saved = tableRepository.save(table);
        log.info("[BRANCH] ACTION={} tenantId={} userId={} resourceId={} resourceType={} status={}",
                BranchAction.TABLE_STATUS_UPDATED, tenantId, userId, saved.getId(), "TABLE", status);

        return TableResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a non-deleted table by ID within the tenant scope.
     * Returns 404 for both missing and cross-tenant resources (IDOR protection).
     */
    private RestaurantTable findActiveByIdAndTenant(UUID tableId, UUID tenantId) {
        return tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Table", tableId));
    }

    /**
     * Validates that the area exists and belongs to the requesting tenant.
     * Returns 404 (not 403) for cross-tenant areas to prevent enumeration.
     */
    private void validateAreaBelongsToTenant(UUID areaId, UUID tenantId) {
        areaRepository.findByIdAndTenantId(areaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Area", areaId));
    }

    /**
     * Validates that a table number is unique within the area.
     *
     * @param number    the number to check
     * @param areaId    the area scope
     * @param tenantId  the tenant scope
     * @param excludeId the table ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if a table with this number already exists
     */
    private void validateTableNumberUniqueness(String number, UUID areaId, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? tableRepository.existsByNumberAndAreaIdAndTenantIdAndIdNot(number, areaId, tenantId, excludeId)
                : tableRepository.existsByNumberAndAreaIdAndTenantId(number, areaId, tenantId);

        if (exists) {
            log.warn("[BRANCH] ACTION=DUPLICATE_TABLE_NUMBER tenantId={} areaId={} number=\"{}\"",
                    tenantId, areaId, number);
            throw new DuplicateResourceException("Table", "number", number);
        }
    }
}
