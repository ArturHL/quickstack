package com.quickstack.product.dto.response;

import java.util.List;

public record MenuResponse(
    List<MenuCategoryItem> categories
) {
    public static MenuResponse of(List<MenuCategoryItem> categories) {
        return new MenuResponse(categories);
    }

    public static MenuResponse empty() {
        return new MenuResponse(List.of());
    }
}
