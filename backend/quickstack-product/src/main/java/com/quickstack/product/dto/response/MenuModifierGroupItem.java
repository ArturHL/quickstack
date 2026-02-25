package com.quickstack.product.dto.response;

import com.quickstack.product.entity.ModifierGroup;
import java.util.List;
import java.util.UUID;

public record MenuModifierGroupItem(
    UUID id,
    String name,
    int minSelections,
    Integer maxSelections,
    boolean isRequired,
    List<MenuModifierItem> modifiers
) {
    public static MenuModifierGroupItem from(ModifierGroup group, List<MenuModifierItem> modifiers) {
        return new MenuModifierGroupItem(
            group.getId(),
            group.getName(),
            group.getMinSelections(),
            group.getMaxSelections(),
            group.isRequired(),
            modifiers
        );
    }
}
