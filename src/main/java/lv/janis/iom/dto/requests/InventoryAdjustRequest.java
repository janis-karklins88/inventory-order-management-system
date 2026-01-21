package lv.janis.iom.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public class InventoryAdjustRequest {

    @Schema(description = "Signed delta applied to quantity (negative allowed)", example = "-5")
    @NotNull
    private Integer delta;

    @Schema(description = "Reason for adjustment", example = "Stock count correction")
    @NotBlank
    private String reason;

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
