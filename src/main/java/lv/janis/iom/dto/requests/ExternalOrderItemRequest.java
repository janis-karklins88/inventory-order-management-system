package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public class ExternalOrderItemRequest {
    @Schema(description = "Product id", example = "42")
    @NotNull
    private Long productId;

    @Schema(description = "Quantity of product", example = "2")
    @Positive
    private int quantity;

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

}
