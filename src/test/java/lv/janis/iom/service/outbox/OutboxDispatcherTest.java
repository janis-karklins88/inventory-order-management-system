package lv.janis.iom.service.outbox;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.FailureCode;
import lv.janis.iom.enums.OutboxEventStatus;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.exception.BusinessException;
import lv.janis.iom.repository.OutboxEventRepository;
import lv.janis.iom.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

  @Mock
  OutboxEventRepository outboxEventRepository;
  @Mock
  OutboxHandlerRegistry outboxHandlerRegistry;
  @Mock
  OrderService orderService;

  @InjectMocks
  OutboxDispatcher dispatcher;

  @Test
  void processClaimed_success_marksProcessedAndClearsLocks() {
    OutboxEvent event = event(100L, OutboxEventStatus.PROCESSING, 0);
    when(outboxEventRepository.findById(100L)).thenReturn(Optional.of(event));

    dispatcher.processClaimed(100L);

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertEquals(OutboxEventStatus.PROCESSED, saved.getStatus());
    assertNotNull(saved.getProcessedAt());
    assertNull(saved.getLastError());
    assertNull(saved.getLockedAt());
    assertNull(saved.getLockedBy());
  }

  @Test
  void processClaimed_businessException_marksProcessedAndClearsLocks() {
    OutboxEvent event = event(101L, OutboxEventStatus.PROCESSING, 0);
    when(outboxEventRepository.findById(101L)).thenReturn(Optional.of(event));
    doThrow(new BusinessException(FailureCode.OUT_OF_STOCK, "no stock"))
        .when(outboxHandlerRegistry).handle(event);

    dispatcher.processClaimed(101L);

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertEquals(OutboxEventStatus.PROCESSED, saved.getStatus());
    assertEquals(0, saved.getAttempts());
    assertNotNull(saved.getProcessedAt());
    assertNull(saved.getLastError());
    assertNull(saved.getLockedAt());
    assertNull(saved.getLockedBy());
  }

  @Test
  void processClaimed_runtimeException_marksFailedAndSchedulesRetry() {
    OutboxEvent event = event(102L, OutboxEventStatus.PROCESSING, 1);
    when(outboxEventRepository.findById(102L)).thenReturn(Optional.of(event));
    doThrow(new RuntimeException("boom")).when(outboxHandlerRegistry).handle(event);
    Instant start = Instant.now();

    dispatcher.processClaimed(102L);

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertEquals(OutboxEventStatus.FAILED, saved.getStatus());
    assertEquals(2, saved.getAttempts());
    assertEquals("Unexpected processing error", saved.getLastError());
    assertNotNull(saved.getAvailableAt());
    assertTrue(saved.getAvailableAt().isAfter(start));
    assertNull(saved.getLockedAt());
    assertNull(saved.getLockedBy());
    verify(orderService, never()).markFailed(any(), any(), anyString());
  }

  @Test
  void processClaimed_runtimeException_atMaxAttempts_marksDead() {
    OutboxEvent event = event(103L, OutboxEventStatus.PROCESSING, 4);
    when(outboxEventRepository.findById(103L)).thenReturn(Optional.of(event));
    doThrow(new RuntimeException("boom")).when(outboxHandlerRegistry).handle(event);

    dispatcher.processClaimed(103L);

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();
    assertEquals(OutboxEventStatus.DEAD, saved.getStatus());
    assertEquals(5, saved.getAttempts());
    verify(orderService).markFailed(
        eq(10L),
        eq(FailureCode.TECHNICAL_ERROR),
        eq("Outbox delivery failed after max retries"));
  }

  @Test
  void dispatch_processesOnlySuccessfullyClaimedCandidates() {
    OutboxEvent claimedEvent = event(202L, OutboxEventStatus.PROCESSING, 0);
    when(outboxEventRepository.findCandidateIds(anyList(), any(Instant.class), any(Instant.class), anyInt(), any()))
        .thenReturn(List.of(201L, 202L));
    when(outboxEventRepository.claim(eq(201L), any(Instant.class), any(Instant.class), anyInt(), anyString()))
        .thenReturn(0);
    when(outboxEventRepository.claim(eq(202L), any(Instant.class), any(Instant.class), anyInt(), anyString()))
        .thenReturn(1);
    when(outboxEventRepository.findById(202L)).thenReturn(Optional.of(claimedEvent));

    dispatcher.dispatch();

    verify(outboxHandlerRegistry).handle(claimedEvent);
    verify(outboxEventRepository).findById(202L);
  }

  private static OutboxEvent event(Long id, OutboxEventStatus status, int attempts) {
    OutboxEvent event = OutboxEvent.pending(OutboxEventType.EXTERNAL_ORDER_INGESTED, 10L, "{\"orderId\":10}");
    event.setId(id);
    event.setStatus(status);
    event.setAttempts(attempts);
    event.setLockedAt(Instant.now());
    event.setLockedBy("node-1");
    return event;
  }
}
