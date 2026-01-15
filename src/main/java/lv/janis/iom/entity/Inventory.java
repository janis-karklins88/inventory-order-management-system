package lv.janis.iom.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Entity(name = "Inventory")
@EntityListeners(AuditingEntityListener.class)
@Table (
    name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_id", columnNames = {"product_id"})
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
        
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int reorderLevel = 0;

    @Column(nullable = false)
    private int clearLowQuantity = 0;

    @Column(nullable = false)
    private boolean isLowQuantity = false;

    protected Inventory() {
    }

    private Inventory(Product product) {
        this.product = product;
        this.quantity = 0;
        this.reservedQuantity = 0;
    }

    public static Inventory createFor(Product product) {
        if (product == null) throw new IllegalArgumentException("product required");
        return new Inventory(product);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public int getClearLowQuantity() {
        return clearLowQuantity;
    }

    public void setClearLowQuantity(int clearLowQuantity) {
        if (clearLowQuantity < 0) throw new IllegalArgumentException("clearLowQuantity cannot be negative");
        this.clearLowQuantity = clearLowQuantity;
    }

    public boolean isLowQuantity() {
        return isLowQuantity;
    }

    public void setIsLowQuantity(boolean isLowQuantity) {
        this.isLowQuantity = isLowQuantity;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) throw new IllegalArgumentException("reorderLevel cannot be negative");
        this.reorderLevel = reorderLevel;
    }


    @Transient
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        this.quantity += amount;
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.quantity - this.reservedQuantity) {
            throw new IllegalArgumentException("not enough available quantity to decrease");
        }
        this.quantity -= amount;
    }

    public void reserveQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.quantity - this.reservedQuantity) {
            throw new IllegalArgumentException("not enough available quantity to reserve");
        }
        this.reservedQuantity += amount;
    }

    public void unreserveQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.reservedQuantity) {
            throw new IllegalArgumentException("not enough reserved quantity to unreserve");
        }
        this.reservedQuantity -= amount;
    }

    public void deductReservedQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (amount > this.reservedQuantity) {
            throw new IllegalArgumentException("not enough reserved quantity to fulfill");
        }
        this.reservedQuantity -= amount;
        this.quantity -= amount;
    }

}