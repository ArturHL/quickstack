package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Category;
import java.util.List;
import java.util.UUID;

public record MenuCategoryItem(
    UUID id,
    String name,
    int sortOrder,
    String imageUrl,
    List<MenuProductItem> products
) {
    public static MenuCategoryItem from(Category category, List<MenuProductItem> products) {
        return new MenuCategoryItem(
            category.getId(),
            category.getName(),
            category.getSortOrder(),
            category.getImageUrl(),
            products
        );
    }
}
