package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;

public class InventoryCreationRequest {

    @Schema(description = "Initial on-hand quantity", example = "100")
    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @Schema(description = "Reorder level threshold", example = "20")
    @NotNull
    @PositiveOrZero
    private Integer reorderLevel;

    @Schema(description = "Clear low-quantity flag when above this value", example = "30")
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
