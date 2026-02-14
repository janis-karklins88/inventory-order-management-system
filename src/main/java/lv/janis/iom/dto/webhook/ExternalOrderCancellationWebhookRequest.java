package lv.janis.iom.dto.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.ExternalOrderCancelResult;

public record ExternalOrderCancellationWebhookRequest(
    @Schema(description = "Internal order id", example = "1001") Long orderId,
    @Schema(description = "External source", example = "WEB_SHOP") String source,
    @Schema(description = "External order id from source system", example = "EXT-100023") String externalOrderId,
    @Schema(description = "Current order status after cancel attempt", example = "CANCELLED") String status,
    @Schema(description = "Cancellation processing outcome", example = "CANCELLED") String result,
    @Schema(description = "Outcome details when cancellation is not possible", example = "Order cannot be cancelled in status SHIPPED") String message) {

  public static ExternalOrderCancellationWebhookRequest from(CustomerOrder order, ExternalOrderCancelResult result) {
    String message = null;
    if (result == ExternalOrderCancelResult.NOT_CANCELABLE && order.getStatus() != null) {
      message = "Order cannot be cancelled in status " + order.getStatus().name();
    }

    return new ExternalOrderCancellationWebhookRequest(
        order.getId(),
        order.getSource() != null ? order.getSource().name() : null,
        order.getExternalOrderId(),
        order.getStatus() != null ? order.getStatus().name() : null,
        result.name(),
        message);
  }
}
