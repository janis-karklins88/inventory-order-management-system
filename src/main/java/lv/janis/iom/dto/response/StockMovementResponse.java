package lv.janis.iom.dto.response;

import java.time.Instant;

import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;

public class StockMovementResponse {
    private Long id;
    private Long inventoryId;
    private Long productId;
    private Long orderId;
    private int delta;
    private String reason;
    private Instant createdAt;
    private MovementType movementType;

    public StockMovementResponse() {
    }

    public StockMovementResponse(
        Long id,
        Long inventoryId,
        Long productId,
        Long orderId,
        int delta,
        String reason,
        Instant createdAt,
        MovementType movementType
    ) {
        this.id = id;
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.orderId = orderId;
        this.delta = delta;
        this.reason = reason;
        this.createdAt = createdAt;
        this.movementType = movementType;
    }

    public static StockMovementResponse from(StockMovement movement) {
        if (movement == null) throw new IllegalArgumentException("movement required");
        Inventory inventory = movement.getInventory();
        Long inventoryId = inventory != null ? inventory.getId() : null;
        Long productId = inventory != null && inventory.getProduct() != null
            ? inventory.getProduct().getId()
            : null;
        return new StockMovementResponse(
            movement.getId(),
            inventoryId,
            productId,
            movement.getOrderId(),
            movement.getDelta(),
            movement.getReason(),
            movement.getCreatedAt(),
            movement.getMovementType()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
}
