package lv.janis.iom.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lv.janis.iom.enums.ExternalOrderSource;

public class ExternalOrderCancelRequest {
  @Schema(description = "Source system identifier", example = "WEB_SHOP")
  @NotNull
  private ExternalOrderSource source;

  @Schema(description = "External order id from source system", example = "EXT-100023")
  @NotBlank
  private String externalOrderId;

  public ExternalOrderSource getSource() {
    return source;
  }

  public String getExternalOrderId() {
    return externalOrderId;
  }
}
