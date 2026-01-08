package lv.janis.iom.entity;
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
@Table(
    name = "customer_orders",
    indexes = {
        @Index(name = "idx_customer_order_status", columnList = "status"),
        @Index(name = "idx_customer_order_created_at", columnList = "created_at")
    }
)
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OrderItem> items = new ArrayList<>();

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

    public void addItem(OrderItem item) {
        if (item == null) throw new IllegalArgumentException("item required");
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

    public void confirm() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Only orders in CREATED status can be confirmed");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an order with no items");
        }
        this.status = OrderStatus.CONFIRMED;
    }
    public void cancel() {
        if (status != OrderStatus.CREATED && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only orders in CREATED or CONFIRMED status can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void ensureModifiable() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Cannot modify order unless it is in CREATED status");
        }
    }

}
