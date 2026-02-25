package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Combo;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a combo in the public menu view.
 * <p>
 * Combos are tenant-level (no category_id) and appear as a top-level list
 * in MenuResponse alongside the category hierarchy.
 */
public record MenuComboItem(
    UUID id,
    String name,
    String description,
    String imageUrl,
    BigDecimal price,
    int sortOrder,
    List<ComboProductEntry> items
) {
    /**
     * A single product entry within a combo as shown in the menu.
     */
    public record ComboProductEntry(
        UUID productId,
        String productName,
        int quantity
    ) {}

    public static MenuComboItem from(Combo combo, List<ComboProductEntry> items) {
        return new MenuComboItem(
            combo.getId(),
            combo.getName(),
            combo.getDescription(),
            combo.getImageUrl(),
            combo.getPrice(),
            combo.getSortOrder(),
            items
        );
    }
}
