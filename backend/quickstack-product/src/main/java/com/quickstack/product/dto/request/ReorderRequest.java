package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReorderRequest(
        @NotNull(message = "Items list cannot be null")
        @Size(min = 1, max = 500, message = "Items list must contain between 1 and 500 elements")
        List<ReorderItem> items
) {
}
