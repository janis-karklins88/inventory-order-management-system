package lv.janis.iom.dto.filters;

import lv.janis.iom.enums.StockStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public class InventoryFilter {
    @Schema(description = "Search by product name or SKU", example = "widget")
    private String q;
    @Schema(description = "Minimum on-hand quantity", example = "10")
    private Integer minQuantity;
    @Schema(description = "Maximum on-hand quantity", example = "200")
    private Integer maxQuantity;
    @Schema(description = "Minimum available quantity", example = "5")
    private Integer minAvailable;
    @Schema(description = "Maximum available quantity", example = "150")
    private Integer maxAvailable;
    @Schema(description = "Stock status filter", example = "LOW")
    private StockStatus stockStatus;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public Integer getMinAvailable() {
        return minAvailable;
    }

    public void setMinAvailable(Integer minAvailable) {
        this.minAvailable = minAvailable;
    }

    public Integer getMaxAvailable() {
        return maxAvailable;
    }

    public void setMaxAvailable(Integer maxAvailable) {
        this.maxAvailable = maxAvailable;
    }

    public StockStatus getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(StockStatus stockStatus) {
        this.stockStatus = stockStatus;
    }

}
