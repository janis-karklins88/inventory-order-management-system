package lv.janis.iom.dto.filters;

import lv.janis.iom.enums.StockStatus;

public class InventoryFilter {
    private String q;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Integer minAvailable;
    private Integer maxAvailable;
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
