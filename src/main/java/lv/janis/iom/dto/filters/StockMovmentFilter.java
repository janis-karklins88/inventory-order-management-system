package lv.janis.iom.dto.filters;

import java.time.Instant;

import lv.janis.iom.enums.MovementType;
import lv.janis.iom.enums.StockMovementDirection;
import io.swagger.v3.oas.annotations.media.Schema;

public class StockMovmentFilter {
    @Schema(description = "Filter by product id", example = "42")
    private Long productId;
    @Schema(description = "Filter by inventory id", example = "2001")
    private Long inventoryId;
    @Schema(description = "Filter from timestamp (inclusive)")
    private Instant from;
    @Schema(description = "Filter to timestamp (inclusive)")
    private Instant to;
    @Schema(description = "Filter by movement type", example = "ORDER_RESERVED")
    private MovementType movementType;
    @Schema(description = "Filter by order id", example = "1001")
    private Long orderId;
    @Schema(description = "Filter by direction", example = "OUT")
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
