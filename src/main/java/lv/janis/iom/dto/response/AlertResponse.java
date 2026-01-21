package lv.janis.iom.dto.response;

import java.time.Instant;

import lv.janis.iom.entity.Alert;
import lv.janis.iom.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;

public record AlertResponse(
    @Schema(description = "Alert id", example = "9001") Long id,
    @Schema(description = "Alert type", example = "LOW_STOCK") AlertType alertType,
    @Schema(description = "Product SKU at alert time", example = "SKU-123") String skuSnapshot,
    @Schema(description = "Product name at alert time", example = "Widget A") String productNameSnapshot,
    @Schema(description = "Available quantity at alert time", example = "4") int availableQuantity,
    @Schema(description = "Threshold at alert time", example = "10") int thresholdSnapshot,
    @Schema(description = "Buffer at alert time", example = "2") int bufferSnapshot,
    @Schema(description = "Creation timestamp") Instant createdAt,
    @Schema(description = "Acknowledged timestamp") Instant acknowledgedAt,
    @Schema(description = "Acknowledged flag", example = "false") boolean acknowledged
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getAlertType(),
            alert.getSkuSnapshot(),
            alert.getProductNameSnapshot(),
            alert.getAvailableQuantity(),
            alert.getThresholdSnapshot(),
            alert.getBufferSnapshot(),
            alert.getCreatedAt(),
            alert.getAcknowledgedAt(),
            alert.isAcknowledged()
        );
    }
}
