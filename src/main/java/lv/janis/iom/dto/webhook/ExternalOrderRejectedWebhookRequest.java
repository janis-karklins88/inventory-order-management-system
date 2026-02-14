package lv.janis.iom.dto.webhook;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lv.janis.iom.entity.CustomerOrder;

public record ExternalOrderRejectedWebhookRequest(
    @Schema(description = "Internal order id", example = "1001") Long orderId,
    @Schema(description = "External source", example = "WEB_SHOP") String source,
    @Schema(description = "External order id from source system", example = "EXT-100023") String externalOrderId,
    @Schema(description = "Current order status", example = "REJECTED") String status,
    @Schema(description = "Failure code", example = "OUT_OF_STOCK") String failureCode,
    @Schema(description = "Failure details", example = "not enough available quantity to reserve") String failureMessage,
    @Schema(description = "UTC timestamp when rejection was recorded", example = "2026-02-13T12:34:56Z") Instant failedAt) {

  public static ExternalOrderRejectedWebhookRequest from(CustomerOrder order) {
    return new ExternalOrderRejectedWebhookRequest(
        order.getId(),
        order.getSource() != null ? order.getSource().name() : null,
        order.getExternalOrderId(),
        order.getStatus() != null ? order.getStatus().name() : null,
        order.getFailureCode() != null ? order.getFailureCode().name() : null,
        order.getFailureMessage(),
        order.getFailedAt());
  }
}
