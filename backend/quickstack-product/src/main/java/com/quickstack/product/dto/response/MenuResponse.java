package com.quickstack.product.dto.response;

import java.util.List;

public record MenuResponse(
    List<MenuCategoryItem> categories,
    List<MenuComboItem> combos
) {
    public static MenuResponse of(List<MenuCategoryItem> categories, List<MenuComboItem> combos) {
        return new MenuResponse(categories, combos);
    }

    public static MenuResponse empty() {
        return new MenuResponse(List.of(), List.of());
    }
}
