package lv.janis.iom.service.outbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.webhook.ExternalOrderWebhookSender;

@ExtendWith(MockitoExtension.class)
class ExternalOrderRejectedHandlerTest {

  @Mock
  OrderService orderService;
  @Mock
  ExternalOrderWebhookSender webhookSender;

  @InjectMocks
  ExternalOrderRejectedHandler handler;

  @Test
  void handle_rejectedOrder_sendsWebhook() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_REJECTED, 15L, "{\"orderId\":15}");
    CustomerOrder order = CustomerOrder.create();
    order.setStatus(OrderStatus.REJECTED);
    when(orderService.getCustomerOrderById(15L)).thenReturn(order);

    handler.handle(event);

    verify(webhookSender).sendRejected(order);
  }

  @Test
  void handle_nonRejectedOrder_noop() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_REJECTED, 16L, "{\"orderId\":16}");
    CustomerOrder order = CustomerOrder.create();
    order.markProcessing();
    when(orderService.getCustomerOrderById(16L)).thenReturn(order);

    handler.handle(event);

    verify(webhookSender, never()).sendRejected(order);
  }

  @Test
  void handle_webhookSenderFails_rethrows() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_REJECTED, 17L, "{\"orderId\":17}");
    CustomerOrder order = CustomerOrder.create();
    order.setStatus(OrderStatus.REJECTED);
    when(orderService.getCustomerOrderById(17L)).thenReturn(order);
    doThrow(new RuntimeException("webhook down")).when(webhookSender).sendRejected(order);

    assertThrows(RuntimeException.class, () -> handler.handle(event));
  }
}
