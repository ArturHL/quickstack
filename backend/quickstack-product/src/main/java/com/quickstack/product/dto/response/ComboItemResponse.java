package com.quickstack.product.dto.response;

import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Product;

import java.util.UUID;

/**
 * Response DTO for a single item within a combo.
 * Includes product name resolved from the product entity.
 */
public record ComboItemResponse(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        boolean allowSubstitutes,
        String substituteGroup,
        int sortOrder
) {
    /**
     * Maps a ComboItem entity and its associated Product to a ComboItemResponse.
     *
     * @param item    the combo item entity
     * @param product the product entity for name resolution
     * @return the response DTO
     */
    public static ComboItemResponse from(ComboItem item, Product product) {
        return new ComboItemResponse(
                item.getId(),
                item.getProductId(),
                product != null ? product.getName() : null,
                item.getQuantity(),
                item.isAllowSubstitutes(),
                item.getSubstituteGroup(),
                item.getSortOrder()
        );
    }
}
