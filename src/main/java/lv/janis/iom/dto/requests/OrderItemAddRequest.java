package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public class OrderItemAddRequest {

    @Schema(description = "Product id", example = "42")
    @NotNull
    private Long productId;

    @Schema(description = "Quantity of product", example = "3")
    @NotNull
    @Positive
    private Integer quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
