package lv.janis.iom.service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.ExternalOrderCancelResult;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.webhook.ExternalOrderWebhookSender;

@Component
public class ExternalOrderCancellationResultHandler {
  private final OrderService orderService;
  private final ExternalOrderWebhookSender webhookSender;
  private final ObjectMapper objectMapper;

  public ExternalOrderCancellationResultHandler(
      OrderService orderService,
      ExternalOrderWebhookSender webhookSender,
      ObjectMapper objectMapper) {
    this.orderService = orderService;
    this.webhookSender = webhookSender;
    this.objectMapper = objectMapper;
  }

  public void handle(OutboxEvent event) {
    Long orderId = event.getAggregatedId();
    var order = orderService.getCustomerOrderById(orderId);
    ExternalOrderCancelResult result = parseResult(event.getPayload());
    webhookSender.sendCancellationResult(order, result);
  }

  private ExternalOrderCancelResult parseResult(String payload) {
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode resultNode = root.get("result");
      if (resultNode == null || resultNode.asText().isBlank()) {
        throw new IllegalStateException("Missing cancel result in payload");
      }
      return ExternalOrderCancelResult.valueOf(resultNode.asText());
    } catch (IllegalStateException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid cancel result payload", ex);
    }
  }
}
