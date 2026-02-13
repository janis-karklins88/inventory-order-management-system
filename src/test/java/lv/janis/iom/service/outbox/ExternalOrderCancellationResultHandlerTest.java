package lv.janis.iom.service.outbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.ExternalOrderCancelResult;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.webhook.ExternalOrderWebhookSender;

@ExtendWith(MockitoExtension.class)
class ExternalOrderCancellationResultHandlerTest {

  @Mock
  OrderService orderService;
  @Mock
  ExternalOrderWebhookSender webhookSender;

  @InjectMocks
  ExternalOrderCancellationResultHandler handler;

  @Test
  void handle_validPayload_sendsCancellationResultWebhook() {
    handler = new ExternalOrderCancellationResultHandler(orderService, webhookSender, new ObjectMapper());
    OutboxEvent event = OutboxEvent.pending(
        OutboxEventType.EXTERNAL_ORDER_CANCEL_RESULT,
        21L,
        "{\"result\":\"CANCELLED\"}");
    CustomerOrder order = CustomerOrder.create();
    when(orderService.getCustomerOrderById(21L)).thenReturn(order);

    handler.handle(event);

    verify(webhookSender).sendCancellationResult(order, ExternalOrderCancelResult.CANCELLED);
  }

  @Test
  void handle_missingResultInPayload_throws() {
    handler = new ExternalOrderCancellationResultHandler(orderService, webhookSender, new ObjectMapper());
    OutboxEvent event = OutboxEvent.pending(
        OutboxEventType.EXTERNAL_ORDER_CANCEL_RESULT,
        22L,
        "{}");
    CustomerOrder order = CustomerOrder.create();
    when(orderService.getCustomerOrderById(22L)).thenReturn(order);

    assertThrows(IllegalStateException.class, () -> handler.handle(event));
  }

  @Test
  void handle_unknownResultInPayload_throws() {
    handler = new ExternalOrderCancellationResultHandler(orderService, webhookSender, new ObjectMapper());
    OutboxEvent event = OutboxEvent.pending(
        OutboxEventType.EXTERNAL_ORDER_CANCEL_RESULT,
        23L,
        "{\"result\":\"INVALID\"}");
    CustomerOrder order = CustomerOrder.create();
    when(orderService.getCustomerOrderById(23L)).thenReturn(order);

    assertThrows(IllegalStateException.class, () -> handler.handle(event));
  }
}
