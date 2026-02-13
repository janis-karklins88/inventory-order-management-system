package lv.janis.iom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-order.webhook")
public class ExternalOrderWebhookProperties {
  private String baseUrl = "http://localhost:8081";
  private String rejectedPath = "/webhooks/external-orders/{source}/rejected";

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getRejectedPath() {
    return rejectedPath;
  }

  public void setRejectedPath(String rejectedPath) {
    this.rejectedPath = rejectedPath;
  }
}
