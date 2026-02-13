package lv.janis.iom.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lv.janis.iom.enums.OutboxEventStatus;

@Entity(name = "OutboxEvent")
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false)
  private Long aggregatedId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxEventStatus status;

  @Lob
  @Column(nullable = false)
  private String payload;

  @Column(nullable = false)
  private int attempts;

  private Instant availableAt;

  private String lastError;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant lockedAt;

  @Column(length = 100)
  private String lockedBy;

  private Instant processedAt;

  protected OutboxEvent() {
  }

  public static OutboxEvent pending(
      String eventType,
      Long aggregatedId,
      String payload) {
    var event = new OutboxEvent();
    event.eventType = eventType;
    event.aggregatedId = aggregatedId;
    event.payload = payload;
    event.status = OutboxEventStatus.PENDING;
    event.attempts = 0;
    event.createdAt = Instant.now();
    event.availableAt = Instant.now();
    return event;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Long getAggregatedId() {
    return aggregatedId;
  }

  public void setAggregatedId(Long aggregatedId) {
    this.aggregatedId = aggregatedId;
  }

  public OutboxEventStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxEventStatus status) {
    this.status = status;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Instant getAvailableAt() {
    return availableAt;
  }

  public void setAvailableAt(Instant availableAt) {
    this.availableAt = availableAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public void setLockedBy(String lockedBy) {
    this.lockedBy = lockedBy;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }

  public void setLockedAt(Instant lockedAt) {
    this.lockedAt = lockedAt;
  }

}
