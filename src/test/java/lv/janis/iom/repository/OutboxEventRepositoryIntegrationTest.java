package lv.janis.iom.repository;

import lv.janis.iom.config.JpaConfig;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OutboxEventStatus;
import lv.janis.iom.enums.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(JpaConfig.class)
class OutboxEventRepositoryIntegrationTest {

  @Autowired
  OutboxEventRepository outboxEventRepository;
  @Autowired
  EntityManager entityManager;

  @Test
  void findCandidateIds_includesPendingFailedAndStaleProcessing() {
    Instant now = Instant.now();
    Instant staleBefore = now.minusSeconds(300);

    OutboxEvent pending = outboxEventRepository.save(event(OutboxEventStatus.PENDING, now.minusSeconds(1), 0, null));
    OutboxEvent failed = outboxEventRepository.save(event(OutboxEventStatus.FAILED, now.minusSeconds(1), 1, null));
    OutboxEvent staleProcessing = outboxEventRepository.save(
        event(OutboxEventStatus.PROCESSING, now.plusSeconds(3600), 2, now.minusSeconds(600)));

    OutboxEvent futurePending = outboxEventRepository.save(event(OutboxEventStatus.PENDING, now.plusSeconds(60), 0, null));
    OutboxEvent freshProcessing = outboxEventRepository.save(
        event(OutboxEventStatus.PROCESSING, now.minusSeconds(1), 2, now.minusSeconds(60)));
    OutboxEvent maxedOut = outboxEventRepository.save(event(OutboxEventStatus.FAILED, now.minusSeconds(1), 5, null));

    List<Long> ids = outboxEventRepository.findCandidateIds(
        List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
        now,
        staleBefore,
        5,
        PageRequest.of(0, 20));

    assertTrue(ids.contains(pending.getId()));
    assertTrue(ids.contains(failed.getId()));
    assertTrue(ids.contains(staleProcessing.getId()));

    assertFalse(ids.contains(futurePending.getId()));
    assertFalse(ids.contains(freshProcessing.getId()));
    assertFalse(ids.contains(maxedOut.getId()));
  }

  @Test
  void claim_eligiblePending_setsProcessingAndLockMetadata() {
    Instant now = Instant.now();
    Instant staleBefore = now.minusSeconds(300);

    OutboxEvent event = outboxEventRepository.save(event(OutboxEventStatus.PENDING, now.minusSeconds(1), 0, null));

    int updated = outboxEventRepository.claim(event.getId(), now, staleBefore, 5, "node-a");
    entityManager.clear();
    OutboxEvent claimed = outboxEventRepository.findById(event.getId()).orElseThrow();

    assertEquals(1, updated);
    assertEquals(OutboxEventStatus.PROCESSING, claimed.getStatus());
    assertEquals("node-a", claimed.getLockedBy());
    assertNotNull(claimed.getLockedAt());
    assertTrue(!claimed.getLockedAt().isBefore(now.minusSeconds(1)));
  }

  @Test
  void claim_futurePending_returnsZero() {
    Instant now = Instant.now();
    Instant staleBefore = now.minusSeconds(300);

    OutboxEvent event = outboxEventRepository.save(event(OutboxEventStatus.PENDING, now.plusSeconds(30), 0, null));

    int updated = outboxEventRepository.claim(event.getId(), now, staleBefore, 5, "node-a");

    assertEquals(0, updated);
  }

  @Test
  void claim_staleProcessing_reclaimsEvent() {
    Instant now = Instant.now();
    Instant staleBefore = now.minusSeconds(300);

    OutboxEvent event = outboxEventRepository.save(
        event(OutboxEventStatus.PROCESSING, now.plusSeconds(120), 2, now.minusSeconds(600)));

    int updated = outboxEventRepository.claim(event.getId(), now, staleBefore, 5, "node-b");
    entityManager.clear();
    OutboxEvent claimed = outboxEventRepository.findById(event.getId()).orElseThrow();

    assertEquals(1, updated);
    assertEquals(OutboxEventStatus.PROCESSING, claimed.getStatus());
    assertEquals("node-b", claimed.getLockedBy());
    assertNotNull(claimed.getLockedAt());
    assertTrue(!claimed.getLockedAt().isBefore(now.minusSeconds(1)));
  }

  private static OutboxEvent event(OutboxEventStatus status, Instant availableAt, int attempts, Instant lockedAt) {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_INGESTED, 1L, "{\"orderId\":1}");
    event.setStatus(status);
    event.setAvailableAt(availableAt);
    event.setAttempts(attempts);
    event.setLockedAt(lockedAt);
    event.setLockedBy(lockedAt != null ? "old-node" : null);
    return event;
  }
}
