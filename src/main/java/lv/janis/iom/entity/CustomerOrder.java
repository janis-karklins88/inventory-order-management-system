package lv.janis.iom.entity;

import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.enums.FailureCode;
import lv.janis.iom.enums.OrderStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity(name = "CustomerOrder")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "customer_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_source_external_id", columnNames = { "source", "external_order_id" })
}, indexes = {
        @Index(name = "idx_customer_order_status", columnList = "status"),
        @Index(name = "idx_customer_order_created_at", columnList = "created_at"),
        @Index(name = "idx_order_source_external_id", columnList = "source, external_order_id")
})
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = true, length = 32)
    private ExternalOrderSource source;

    @Column(name = "external_order_id", nullable = true, length = 64)
    private String externalOrderId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = true, length = 128)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", nullable = true, length = 32)
    private FailureCode failureCode;

    @Column(name = "failure_message", nullable = true, length = 500)
    private String failureMessage;

    @Column(name = "retry_count", nullable = true)
    private Integer retryCount = 0;

    @Column(name = "failed_at", nullable = true)
    private Instant failedAt;

    protected CustomerOrder() {
    }

    private CustomerOrder(OrderStatus status) {
        this.status = status;
        this.totalAmount = BigDecimal.ZERO;

    }

    public static CustomerOrder create() {
        return new CustomerOrder(OrderStatus.CREATED);
    }

    public Long getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public ExternalOrderSource getSource() {
        return source;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public FailureCode getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(FailureCode failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setSource(ExternalOrderSource source) {
        if (this.source != null) {
            throw new IllegalStateException("Source is already set and cannot be modified");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source required");
        }
        ensureModifiable();
        this.source = source;
    }

    public void setExternalOrderId(String externalOrderId) {
        if (this.externalOrderId != null) {
            throw new IllegalStateException("External Order ID is already set and cannot be modified");
        }
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new IllegalArgumentException("External Order ID required");
        }
        ensureModifiable();
        this.externalOrderId = externalOrderId;
    }

    public void setShippingAddress(String shippingAddress) {
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Shipping address cannot be null or blank");
        }
        ensureModifiable();
        this.shippingAddress = shippingAddress;
    }

    public void addItem(OrderItem item) {
        if (item == null)
            throw new IllegalArgumentException("item required");
        ensureModifiable();
        items.add(item);
        item.attachTo(this);
        recalculateTotalAmount();
    }

    public void removeItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        ensureModifiable();

        boolean removed = items.remove(item);
        if (removed) {
            item.detach();
            recalculateTotalAmount();
        }
    }

    private void recalculateTotalAmount() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            sum = sum.add(item.getTotalPrice());
        }
        this.totalAmount = sum;
    }

    public void markProcessing() {
        if (status != OrderStatus.CREATED)
            throw new IllegalStateException("Only CREATED can go to PROCESSING");
        status = OrderStatus.PROCESSING;
    }

    public void markShipped() {
        if (status != OrderStatus.PROCESSING)
            throw new IllegalStateException("Only PROCESSING can go to SHIPPED");
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        if (status != OrderStatus.SHIPPED)
            throw new IllegalStateException("Only SHIPPED can go to DELIVERED");
        status = OrderStatus.DELIVERED;
    }

    public void markCancelled() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED)
            throw new IllegalStateException("Cannot cancel after shipped");
        status = OrderStatus.CANCELLED;
    }

    public void markReturned() {
        if (status != OrderStatus.DELIVERED)
            throw new IllegalStateException("Only DELIVERED can go to RETURNED");
        status = OrderStatus.RETURNED;
    }

    private void ensureModifiable() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot modify order unless it is in CREATED status");
        }
    }

}
