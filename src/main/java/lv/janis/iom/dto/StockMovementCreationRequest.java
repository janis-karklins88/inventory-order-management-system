package lv.janis.iom.dto;

import jakarta.persistence.criteria.CriteriaBuilder.In;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.enums.MovementType;

public class StockMovementCreationRequest {
    private Inventory inventory;
    private MovementType movementType;
    private int delta;
    private String reason;
    private Long orderId;

    public Inventory getInventory() {
        return inventory;
    }
    public MovementType getMovementType() {
        return movementType;
    }
    public int getDelta() {
        return delta;
    }
    public String getReason() {
        return reason;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
    public void setDelta(int delta) {
        this.delta = delta;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
}