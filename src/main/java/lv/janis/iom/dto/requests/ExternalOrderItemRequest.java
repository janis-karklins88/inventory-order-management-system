package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ExternalOrderItemRequest {
    @NotNull
    private Long productId;

    @Positive
    private int quantity;

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

}
