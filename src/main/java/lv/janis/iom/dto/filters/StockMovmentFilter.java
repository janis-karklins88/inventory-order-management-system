package lv.janis.iom.dto.filters;

import java.time.Instant;

import lv.janis.iom.enums.MovementType;
import lv.janis.iom.enums.StockMovementDirection;

public class StockMovmentFilter {
    private Long productId;
    private Long inventoryId;
    private Instant from;
    private Instant to;
    private MovementType movementType;
    private Long orderId;
    private StockMovementDirection direction;

    public Long getProductId() {
        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }
    public Instant getFrom() {
        return from;
    }
    public void setFrom(Instant from) {
        this.from = from;
    }
    public Instant getTo() {
        return to;
    }
    public void setTo(Instant to) {
        this.to = to;
    }
    public MovementType getMovementType() {
        return movementType;
    }
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
    public Long getOrderId() {
        return orderId;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public StockMovementDirection getDirection() {
        return direction;
    }
    public void setDirection(StockMovementDirection direction) {
        this.direction = direction;
    }
}