package lv.janis.iom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lv.janis.iom.entity.CustomerOrder;

public record ExternalOrderStatusResponse(
    @Schema(description = "Internal order id", example = "1001") Long orderId,
    @Schema(description = "External source", example = "WEB_SHOP") String source,
    @Schema(description = "External order id", example = "EXT-100023") String externalOrderId,
    @Schema(description = "Current order status", example = "PROCESSING") String status,
    @Schema(description = "Error code when rejected/failed", example = "OUT_OF_STOCK") String errorCode,
    @Schema(description = "Error details when rejected/failed", example = "not enough available quantity to reserve") String errorMessage) {

  public static ExternalOrderStatusResponse from(CustomerOrder order) {
    return new ExternalOrderStatusResponse(
        order.getId(),
        order.getSource() != null ? order.getSource().name() : null,
        order.getExternalOrderId(),
        order.getStatus() != null ? order.getStatus().name() : null,
        order.getFailureCode() != null ? order.getFailureCode().name() : null,
        order.getFailureMessage());
  }
}
