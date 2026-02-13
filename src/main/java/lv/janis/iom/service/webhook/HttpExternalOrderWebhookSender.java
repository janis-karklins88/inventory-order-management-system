package lv.janis.iom.service.webhook;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lv.janis.iom.config.ExternalOrderWebhookProperties;
import lv.janis.iom.dto.webhook.ExternalOrderCancellationWebhookRequest;
import lv.janis.iom.dto.webhook.ExternalOrderRejectedWebhookRequest;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.ExternalOrderCancelResult;

@Component
public class HttpExternalOrderWebhookSender implements ExternalOrderWebhookSender {
  private final RestTemplate restTemplate;
  private final String webhookBaseUrl;
  private final String rejectedPathTemplate;
  private final String cancelPathTemplate;

  public HttpExternalOrderWebhookSender(
      RestTemplateBuilder restTemplateBuilder,
      ExternalOrderWebhookProperties properties) {
    this.restTemplate = restTemplateBuilder.build();
    this.webhookBaseUrl = properties.getBaseUrl();
    this.rejectedPathTemplate = properties.getRejectedPath();
    this.cancelPathTemplate = properties.getCancelPath();
  }

  @Override
  public void sendRejected(CustomerOrder order) {
    if (order.getSource() == null) {
      throw new IllegalStateException("Order source is required for webhook push");
    }

    var payload = ExternalOrderRejectedWebhookRequest.from(order);
    var endpoint = UriComponentsBuilder.fromUriString(webhookBaseUrl)
        .path(rejectedPathTemplate)
        .buildAndExpand(order.getSource().name())
        .toUri();

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    var request = new HttpEntity<>(payload, headers);

    restTemplate.postForEntity(endpoint, request, Void.class);
  }

  @Override
  public void sendCancellationResult(CustomerOrder order, ExternalOrderCancelResult result) {
    if (order.getSource() == null) {
      throw new IllegalStateException("Order source is required for webhook push");
    }

    var payload = ExternalOrderCancellationWebhookRequest.from(order, result);
    var endpoint = UriComponentsBuilder.fromUriString(webhookBaseUrl)
        .path(cancelPathTemplate)
        .buildAndExpand(order.getSource().name())
        .toUri();

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    var request = new HttpEntity<>(payload, headers);

    restTemplate.postForEntity(endpoint, request, Void.class);
  }
}
