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
import jakarta.persistence.Table;
import lv.janis.iom.enums.NotificationTaskStatus;

@Entity(name = "NotificationTask")
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification_tasks")
public class NotificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private final String taskName = "LOW_STOCK_ALERT";


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)    
    private Inventory inventory;

    private int attempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTaskStatus status = NotificationTaskStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name  = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    protected NotificationTask() {}

    public NotificationTask(Inventory inventory) {
        this.inventory = inventory;
        this.nextAttemptAt = Instant.now();
    }

    public Long getId() {
        return id;
    }
    public String getTaskName() {
        return taskName;
    }
    public Inventory getInventory() {
        return inventory;
    }
    public int getAttempts() {
        return attempts;
    }
    public NotificationTaskStatus getStatus() {
        return status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }
    public void incrementAttempts() {
        this.attempts++;
    }
    public void setStatus(NotificationTaskStatus status) {
        this.status = status;
    }
    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }
    

}
