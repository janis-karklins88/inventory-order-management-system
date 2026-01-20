package lv.janis.iom.dto.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lv.janis.iom.enums.ExternalOrderSource;

public class ExternalOrderIngestRequest {
    @NotNull
    private ExternalOrderSource source;
    @NotBlank
    private String externalOrderId;
    @NotBlank
    private String shippingAddress;
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
