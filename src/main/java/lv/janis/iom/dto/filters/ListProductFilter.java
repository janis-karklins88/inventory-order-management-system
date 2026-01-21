package lv.janis.iom.dto.filters;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class ListProductFilter {

    @Schema(description = "Search by name or description", example = "stainless")
    @Size(max = 200)
    private String query;

    @Schema(description = "Exact SKU match", example = "SKU-ABC-001")
    @Size(max = 64)
    private String sku;

    @Schema(description = "Minimum price", example = "10.00")
    @PositiveOrZero
    private BigDecimal minPrice;

    @Schema(description = "Maximum price", example = "250.00")
    @PositiveOrZero
    private BigDecimal maxPrice;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }
}
