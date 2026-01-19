package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class InventoryCreationRequest {

    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    private Integer reorderLevel;

    @PositiveOrZero
    private Integer clearLowQuantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Integer getClearLowQuantity() {
        return clearLowQuantity;
    }

    public void setClearLowQuantity(Integer clearLowQuantity) {
        this.clearLowQuantity = clearLowQuantity;
    }
}
