package com.quickstack.branch.dto.response;

import com.quickstack.branch.entity.Area;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an area, including core info and audit metadata.
 */
public record AreaResponse(
    UUID id,
    UUID tenantId,
    UUID branchId,
    String name,
    String description,
    int sortOrder,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
    /**
     * Maps an Area entity to an AreaResponse.
     *
     * @param area the entity to map
     * @return the response DTO
     */
    public static AreaResponse from(Area area) {
        return new AreaResponse(
            area.getId(),
            area.getTenantId(),
            area.getBranchId(),
            area.getName(),
            area.getDescription(),
            area.getSortOrder(),
            area.isActive(),
            area.getCreatedAt(),
            area.getUpdatedAt(),
            area.getCreatedBy(),
            area.getUpdatedBy()
        );
    }
}
