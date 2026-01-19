package lv.janis.iom.entity;

import jakarta.persistence.Transient;
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
import jakarta.persistence.Table;
import lv.janis.iom.enums.AlertType;

@Entity(name = "Alert")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(nullable = false)
    private String skuSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int thresholdSnapshot;

    @Column(nullable = false)
    private int bufferSnapshot;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant acknowledgedAt;

    protected Alert() {
    }

   protected Alert(
        AlertType alertType,
        Inventory inventory,
        String skuSnapshot,
        String productNameSnapshot,
        int availableQuantity,
        int thresholdSnapshot,
        int bufferSnapshot
    ) {
        this.alertType = alertType;
        this.inventory = inventory;
        this.skuSnapshot = skuSnapshot;
        this.productNameSnapshot = productNameSnapshot;
        this.availableQuantity = availableQuantity;
        this.thresholdSnapshot = thresholdSnapshot;
        this.bufferSnapshot = bufferSnapshot;
    }

    public static Alert createLowStockAlert(Inventory inventory) {

        return new Alert(
            AlertType.LOW_STOCK,
            inventory,
            inventory.getProduct().getSku(),
            inventory.getProduct().getName(),
            inventory.getAvailableQuantity(),
            inventory.getReorderLevel(),
            inventory.getClearLowQuantity()
        );
    }
   

    public Long getId() {
        return id;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getThresholdSnapshot() {
        return thresholdSnapshot;
    }

    public int getBufferSnapshot() {
        return bufferSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

 
    @Transient
    public boolean isAcknowledged() {
        return this.acknowledgedAt != null;
    }

    public void acknowledge(Instant timestamp) {
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = timestamp;
        }
    }
}
