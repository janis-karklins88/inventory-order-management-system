package lv.janis.iom.dto.response;

import java.time.Instant;

import lv.janis.iom.entity.Alert;
import lv.janis.iom.enums.AlertType;

public record AlertResponse(
    Long id,
    AlertType alertType,
    String skuSnapshot,
    String productNameSnapshot,
    int availableQuantity,
    int thresholdSnapshot,
    int bufferSnapshot,
    Instant createdAt,
    Instant acknowledgedAt,
    boolean acknowledged
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
