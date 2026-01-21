package lv.janis.iom.dto.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lv.janis.iom.enums.ExternalOrderSource;

public class ExternalOrderIngestRequest {
    @Schema(description = "Source system identifier", example = "E-SHOP")
    @NotNull
    private ExternalOrderSource source;
    @Schema(description = "External order id from source system", example = "EXT-100023")
    @NotBlank
    private String externalOrderId;
    @Schema(description = "Shipping address as provided by source", example = "Brivibas iela 100, Riga, LV-1011")
    @NotBlank
    private String shippingAddress;
    @Schema(description = "Line items for the order")
    @NotEmpty @Valid
    private List<ExternalOrderItemRequest> items;

    public ExternalOrderSource getSource() {
        return source;
    }


    public String getExternalOrderId() {
        return externalOrderId;
    }


    public String getShippingAddress() {
        return shippingAddress;
    }

    public List<ExternalOrderItemRequest> getItems() {
        return items;
    }

}
