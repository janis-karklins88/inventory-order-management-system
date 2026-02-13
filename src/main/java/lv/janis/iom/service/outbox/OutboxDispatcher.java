package lv.janis.iom.service.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OutboxEventStatus;
import lv.janis.iom.exception.BusinessException;
import lv.janis.iom.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OutboxDispatcher {
  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

  private final OutboxEventRepository repo;
  private final OutboxHandlerRegistry handlers;
  private final String lockedBy;

  private final int batchSize = 20;
  private final int maxAttempts = 5;
  private final int processingLockTimeoutSeconds = 300;

  public OutboxDispatcher(OutboxEventRepository repo, OutboxHandlerRegistry handlers) {
    this.repo = repo;
    this.handlers = handlers;
    this.lockedBy = java.util.UUID.randomUUID().toString(); // or hostname
  }

  @Scheduled(fixedDelay = 20000)
  public void dispatch() {
    Instant now = Instant.now();
    Instant staleBefore = now.minusSeconds(processingLockTimeoutSeconds);

    List<Long> candidates = repo.findCandidateIds(
        List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
        now,
        staleBefore,
        maxAttempts,
        PageRequest.of(0, batchSize));

    for (Long id : candidates) {
      int claimed = repo.claim(id, now, staleBefore, maxAttempts, lockedBy);
      if (claimed != 1)
        continue;

      processClaimed(id);
    }
  }

  protected void processClaimed(Long outboxId) {
    OutboxEvent event = repo.findById(outboxId).orElseThrow();

    try {
      handlers.handle(event); // calls the right handler by eventType

      event.setStatus(OutboxEventStatus.PROCESSED);
      event.setProcessedAt(Instant.now());
      event.setLastError(null);

    } catch (BusinessException be) {
      // business outcome: treat as processed (handler should mark order REJECTED)
      event.setStatus(OutboxEventStatus.PROCESSED);
      event.setProcessedAt(Instant.now());
      event.setLastError(null);

    } catch (Exception ex) {
      log.error("Outbox processing failed id={} type={}", event.getId(), event.getEventType(), ex);

      event.setAttempts(event.getAttempts() + 1);
      event.setStatus(event.getAttempts() >= maxAttempts ? OutboxEventStatus.DEAD : OutboxEventStatus.FAILED);
      event.setLastError("Unexpected processing error");

      // simple backoff: 2^attempts seconds, capped
      long delaySeconds = Math.min(300, (long) Math.pow(2, Math.min(10, event.getAttempts())));
      event.setAvailableAt(Instant.now().plusSeconds(delaySeconds));
    } finally {
      // release lock metadata (optional; status is the real lock)
      event.setLockedAt(null);
      event.setLockedBy(null);
      repo.save(event);
    }
  }
}
