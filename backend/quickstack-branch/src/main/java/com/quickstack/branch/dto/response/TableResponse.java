package com.quickstack.branch.dto.response;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a table, including status, position, and audit metadata.
 */
public record TableResponse(
    UUID id,
    UUID tenantId,
    UUID areaId,
    String number,
    String name,
    Integer capacity,
    TableStatus status,
    int sortOrder,
    Integer positionX,
    Integer positionY,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
    /**
     * Maps a RestaurantTable entity to a TableResponse.
     *
     * @param table the entity to map
     * @return the response DTO
     */
    public static TableResponse from(RestaurantTable table) {
        return new TableResponse(
            table.getId(),
            table.getTenantId(),
            table.getAreaId(),
            table.getNumber(),
            table.getName(),
            table.getCapacity(),
            table.getStatus(),
            table.getSortOrder(),
            table.getPositionX(),
            table.getPositionY(),
            table.isActive(),
            table.getCreatedAt(),
            table.getUpdatedAt(),
            table.getCreatedBy(),
            table.getUpdatedBy()
        );
    }
}
