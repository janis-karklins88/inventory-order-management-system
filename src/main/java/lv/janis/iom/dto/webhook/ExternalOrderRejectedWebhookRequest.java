package lv.janis.iom.dto.webhook;

import java.time.Instant;

import lv.janis.iom.entity.CustomerOrder;

public record ExternalOrderRejectedWebhookRequest(
    Long orderId,
    String source,
    String externalOrderId,
    String status,
    String failureCode,
    String failureMessage,
    Instant failedAt) {

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
