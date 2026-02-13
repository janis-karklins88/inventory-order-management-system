package lv.janis.iom.service.outbox;

import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.webhook.ExternalOrderWebhookSender;

@Component
public class ExternalOrderRejectedHandler {
  private final OrderService orderService;
  private final ExternalOrderWebhookSender webhookSender;

  public ExternalOrderRejectedHandler(OrderService orderService, ExternalOrderWebhookSender webhookSender) {
    this.orderService = orderService;
    this.webhookSender = webhookSender;
  }

  public void handle(OutboxEvent event) {
    Long orderId = event.getAggregatedId();

    var order = orderService.getCustomerOrderById(orderId);
    if (order.getStatus() != OrderStatus.REJECTED) {
      return;
    }

    webhookSender.sendRejected(order);
  }
}
