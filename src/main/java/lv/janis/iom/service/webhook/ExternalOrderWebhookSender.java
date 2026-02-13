package lv.janis.iom.service.webhook;

import lv.janis.iom.entity.CustomerOrder;

public interface ExternalOrderWebhookSender {
  void sendRejected(CustomerOrder order);
}
