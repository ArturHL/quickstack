package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Modifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an individual modifier option.
 */
public record ModifierResponse(
        UUID id,
        UUID tenantId,
        UUID modifierGroupId,
        String name,
        BigDecimal priceAdjustment,
        boolean isDefault,
        boolean isActive,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Maps a Modifier entity to a ModifierResponse.
     *
     * @param modifier the entity to map
     * @return the response DTO
     */
    public static ModifierResponse from(Modifier modifier) {
        return new ModifierResponse(
                modifier.getId(),
                modifier.getTenantId(),
                modifier.getModifierGroupId(),
                modifier.getName(),
                modifier.getPriceAdjustment(),
                modifier.isDefault(),
                modifier.isActive(),
                modifier.getSortOrder(),
                modifier.getCreatedAt(),
                modifier.getUpdatedAt()
        );
    }
}
