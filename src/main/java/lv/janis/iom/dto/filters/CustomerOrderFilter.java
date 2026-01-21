package lv.janis.iom.dto.filters;

import java.time.Instant;

import lv.janis.iom.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public class CustomerOrderFilter {
    @Schema(description = "Filter by order status", example = "PROCESSING")
    private OrderStatus status;

    @Schema(description = "Created after timestamp (inclusive)")
    private Instant createdAfter;

    @Schema(description = "Created before timestamp (inclusive)")
    private Instant createdBefore;

    @Schema(description = "Updated after timestamp (inclusive)")
    private Instant updatedAfter;

    @Schema(description = "Updated before timestamp (inclusive)")
    private Instant updatedBefore;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAfter() {
        return createdAfter;
    }
    public void setCreatedAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
    }
    public Instant getCreatedBefore() {
        return createdBefore;
    }
    public void setCreatedBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Instant getUpdatedAfter() {
        return updatedAfter;
    }

    public void setUpdatedAfter(Instant updatedAfter) {
        this.updatedAfter = updatedAfter;
    }

    public Instant getUpdatedBefore() {
        return updatedBefore;
    }

    public void setUpdatedBefore(Instant updatedBefore) {
        this.updatedBefore = updatedBefore;
    }
    
}
