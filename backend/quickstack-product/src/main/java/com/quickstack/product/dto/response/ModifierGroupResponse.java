package com.quickstack.product.dto.response;

import com.quickstack.product.entity.ModifierGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a modifier group, including its list of modifiers.
 */
public record ModifierGroupResponse(
        UUID id,
        UUID tenantId,
        UUID productId,
        String name,
        String description,
        int minSelections,
        Integer maxSelections,
        boolean isRequired,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        List<ModifierResponse> modifiers
) {
    /**
     * Maps a ModifierGroup entity and its modifiers to a ModifierGroupResponse.
     *
     * @param group     the entity to map
     * @param modifiers the list of modifier responses
     * @return the response DTO
     */
    public static ModifierGroupResponse from(ModifierGroup group, List<ModifierResponse> modifiers) {
        return new ModifierGroupResponse(
                group.getId(),
                group.getTenantId(),
                group.getProductId(),
                group.getName(),
                group.getDescription(),
                group.getMinSelections(),
                group.getMaxSelections(),
                group.isRequired(),
                group.getSortOrder(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                modifiers
        );
    }
}
