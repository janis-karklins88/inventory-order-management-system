package lv.janis.iom.dto.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProductCreationRequest {

    @Schema(description = "Unique SKU for the product", example = "SKU-ABC-001")
    @NotBlank
    @Size(max = 64)
    private String sku;

    @Schema(description = "Product name", example = "Widget A")
    @NotBlank
    @Size(max = 200)
    private String name;

    @Schema(description = "Product description", example = "Compact stainless steel widget")
    @Size(max = 2000)
    private String description;

    @Schema(description = "Unit price", example = "19.99")
    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
