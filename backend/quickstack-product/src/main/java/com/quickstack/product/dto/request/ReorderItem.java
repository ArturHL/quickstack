package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReorderItem(
        @NotNull(message = "Item ID cannot be null")
        UUID id,

        @Min(value = 0, message = "Sort order cannot be negative")
        int sortOrder
) {
}
