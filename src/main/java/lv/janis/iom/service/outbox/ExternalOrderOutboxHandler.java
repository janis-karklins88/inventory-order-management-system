package lv.janis.iom.service.outbox;

import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.exception.BusinessException;
import lv.janis.iom.service.OrderService;

@Component
public class ExternalOrderOutboxHandler {
  private final OrderService orderService;

  public ExternalOrderOutboxHandler(OrderService orderService) {
    this.orderService = orderService;
  }

  public void handle(OutboxEvent event) {
    Long orderId = event.getAggregatedId();

    var order = orderService.getCustomerOrderById(orderId);
    if (order.getStatus() != OrderStatus.CREATED) {
      return;
    }

    try {
      orderService.statusProcessing(orderId);
    } catch (BusinessException ex) {
      orderService.markRejected(orderId, ex.getCode(), ex.getMessage());
      throw ex;
    }
  }
}
