package lv.janis.iom.dto.requests;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public class OrderReturnRequest {
  @Schema(description = "Product IDs to return. If omitted or empty, all order items are returned", example = "[42, 43]")
  private List<Long> productIds;

  public List<Long> getProductIds() {
    return productIds;
  }

  public void setProductIds(List<Long> productIds) {
    this.productIds = productIds;
  }
}

