package lv.janis.iom.dto.webhook;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.ExternalOrderCancelResult;

public record ExternalOrderCancellationWebhookRequest(
    Long orderId,
    String source,
    String externalOrderId,
    String status,
    String result,
    String message) {

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
