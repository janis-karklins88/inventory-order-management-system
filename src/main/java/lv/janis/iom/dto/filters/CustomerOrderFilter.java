package lv.janis.iom.dto.filters;

import java.time.Instant;

import lv.janis.iom.enums.OrderStatus;

public class CustomerOrderFilter {
    private OrderStatus status;

    private Instant createdAfter;

    private Instant createdBefore;

    private Instant updatedAfter;

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
