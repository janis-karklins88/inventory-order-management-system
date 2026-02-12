package lv.janis.iom.service.processing;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lv.janis.iom.enums.FailureCode;
import lv.janis.iom.event.ExternalOrderIngestedEvent;
import lv.janis.iom.exception.BusinessException;
import lv.janis.iom.service.OrderService;

@Component
public class ExternalOrderProcessor {
  private final OrderService orderService;

  public ExternalOrderProcessor(OrderService orderService) {
    this.orderService = orderService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ExternalOrderIngestedEvent event) {

    try {
      orderService.statusProcessing(event.orderId());

    } catch (BusinessException ex) {
      orderService.markRejected(event.orderId(), ex.getCode(), ex.getMessage());

    } catch (Exception ex) {
      orderService.markFailed(event.orderId(), FailureCode.TECHNICAL_ERROR, "Unexpected error: " );
    }
  }
}
