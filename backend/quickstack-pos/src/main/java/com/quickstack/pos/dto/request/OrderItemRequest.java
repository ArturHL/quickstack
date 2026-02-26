package com.quickstack.pos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for a single line item within an order.
 * <p>
 * Product and price data is passed explicitly rather than resolved from the catalog
 * at order time. This allows the caller (POS terminal) to present the current
 * price from the menu and snapshot it into the order. Validation that the product
 * exists and is active is performed at the service layer.
 * <p>
 * Business Rules:
 * - Exactly one of productId or comboId must be present (XOR constraint)
 * - variantId is optional and only meaningful when productId is present
 * - modifiers list may be empty but must not be null
 */
public record OrderItemRequest(

    // Exactly one of productId / comboId must be non-null (validated by @AssertTrue)
    UUID productId,

    // Optional — only meaningful when productId is present
    UUID variantId,

    UUID comboId,

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    String productName,

    @Size(max = 100, message = "Variant name must not exceed 100 characters")
    String variantName,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be zero or greater")
    BigDecimal unitPrice,

    @NotNull(message = "Modifiers list is required")
    @Valid
    List<OrderItemModifierRequest> modifiers,

    String notes
) {

    /**
     * Cross-field validation: exactly one of productId or comboId must be present.
     * XOR logic: true only when exactly one is non-null.
     */
    @AssertTrue(message = "Exactly one of productId or comboId must be present")
    public boolean isProductOrComboPresent() {
        return (productId != null) != (comboId != null);
    }
}
