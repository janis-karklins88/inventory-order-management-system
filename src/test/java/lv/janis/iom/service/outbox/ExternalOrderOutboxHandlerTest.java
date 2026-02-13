package lv.janis.iom.service.outbox;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.FailureCode;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.exception.BusinessException;
import lv.janis.iom.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOrderOutboxHandlerTest {

  @Mock
  OrderService orderService;

  @InjectMocks
  ExternalOrderOutboxHandler handler;

  @Test
  void handle_createdOrder_callsStatusProcessing() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_INGESTED, 10L, "{\"orderId\":10}");
    CustomerOrder order = CustomerOrder.create();
    when(orderService.getCustomerOrderById(10L)).thenReturn(order);

    handler.handle(event);

    verify(orderService).statusProcessing(10L);
  }

  @Test
  void handle_nonCreatedOrder_noop() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_INGESTED, 11L, "{\"orderId\":11}");
    CustomerOrder order = CustomerOrder.create();
    order.markProcessing();
    when(orderService.getCustomerOrderById(11L)).thenReturn(order);

    handler.handle(event);

    verify(orderService, never()).statusProcessing(11L);
    verify(orderService, never()).markRejected(11L, FailureCode.OUT_OF_STOCK, "no stock");
  }

  @Test
  void handle_businessException_marksRejectedAndRethrows() {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_INGESTED, 12L, "{\"orderId\":12}");
    CustomerOrder order = CustomerOrder.create();
    BusinessException businessException = new BusinessException(FailureCode.OUT_OF_STOCK, "no stock");
    when(orderService.getCustomerOrderById(12L)).thenReturn(order);
    when(orderService.statusProcessing(12L)).thenThrow(businessException);

    assertThrows(BusinessException.class, () -> handler.handle(event));

    verify(orderService).markRejected(12L, FailureCode.OUT_OF_STOCK, "no stock");
  }
}
