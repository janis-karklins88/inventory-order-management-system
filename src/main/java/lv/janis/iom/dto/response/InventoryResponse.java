package lv.janis.iom.dto.response;

import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;

public class InventoryResponse {
    private Long id;
    private String name;
    private Integer quantity;
    private Product product;
    private Integer reservedQuantity;
    private Integer reorderLevel;
    private Integer clearLowQuantity;
    private Boolean isLowQuantity;

    public InventoryResponse() {
    }

    public InventoryResponse(
        Long id,
        String name,
        Integer quantity,
        Product product,
        Integer reservedQuantity,
        Integer reorderLevel,
        Integer clearLowQuantity,
        Boolean isLowQuantity
    ) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.product = product;
        this.reservedQuantity = reservedQuantity;
        this.reorderLevel = reorderLevel;
        this.clearLowQuantity = clearLowQuantity;
        this.isLowQuantity = isLowQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    public Integer getReorderLevel() {
        return reorderLevel;
    }
    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
    public Integer getClearLowQuantity() {
        return clearLowQuantity;
    }
    public void setClearLowQuantity(Integer clearLowQuantity) {
        this.clearLowQuantity = clearLowQuantity;
    }
    public Boolean getIsLowQuantity() {
        return isLowQuantity;
    }
    public void setIsLowQuantity(Boolean isLowQuantity) {
        this.isLowQuantity = isLowQuantity;
    }

    public static InventoryResponse from(Inventory inventory) {
        if (inventory == null) throw new IllegalArgumentException("inventory required");
        Product product = inventory.getProduct();
        String name = product != null ? product.getName() : null;
        return new InventoryResponse(
            inventory.getId(),
            name,
            inventory.getQuantity(),
            product,
            inventory.getReservedQuantity(),
            inventory.getReorderLevel(),
            inventory.getClearLowQuantity(),
            inventory.isLowQuantity()
        );
    }

}
