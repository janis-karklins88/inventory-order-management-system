package lv.janis.iom.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lv.janis.iom.enums.MovementType;

@Entity(name = "StockMovement")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(nullable = false)
    private int delta;

    private String reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    protected StockMovement() {
    }

    public StockMovement(Inventory inventory, int delta, String reason, Long orderId, MovementType movementType) {
        this.inventory = inventory;
        this.delta = delta;
        this.reason = reason;
        this.orderId = orderId;
        this.movementType = movementType;
    }   

    public Long getId() {
        return id;
    }

    public Inventory getInventory() {
        return inventory;
    }
    public int getDelta() {
        return delta;
    }
    public String getReason() {
        return reason;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Long getOrderId() {
        return orderId;
    }
    public MovementType getMovementType() {
        return movementType;
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
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
    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }
        
    @PrePersist
    @PreUpdate
    private void validateOrderId() {
        if (movementType != MovementType.MANUAL_ADJUSTMENT && orderId == null) {
            throw new IllegalStateException("orderId is required for " + movementType);
        }
    }

    }
