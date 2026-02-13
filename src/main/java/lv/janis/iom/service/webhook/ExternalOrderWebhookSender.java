package lv.janis.iom.service.webhook;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.ExternalOrderCancelResult;

public interface ExternalOrderWebhookSender {
  void sendRejected(CustomerOrder order);

  void sendCancellationResult(CustomerOrder order, ExternalOrderCancelResult result);
}
