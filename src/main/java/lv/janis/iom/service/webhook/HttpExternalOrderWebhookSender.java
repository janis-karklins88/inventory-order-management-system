package lv.janis.iom.service.webhook;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lv.janis.iom.config.ExternalOrderWebhookProperties;
import lv.janis.iom.dto.webhook.ExternalOrderRejectedWebhookRequest;
import lv.janis.iom.entity.CustomerOrder;

@Component
public class HttpExternalOrderWebhookSender implements ExternalOrderWebhookSender {
  private final RestTemplate restTemplate;
  private final String webhookBaseUrl;
  private final String rejectedPathTemplate;

  public HttpExternalOrderWebhookSender(
      RestTemplateBuilder restTemplateBuilder,
      ExternalOrderWebhookProperties properties) {
    this.restTemplate = restTemplateBuilder.build();
    this.webhookBaseUrl = properties.getBaseUrl();
    this.rejectedPathTemplate = properties.getRejectedPath();
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
}
