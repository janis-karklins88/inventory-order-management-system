package lv.janis.iom.notification;

import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.NotificationTask;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.NotificationTaskStatus;
import lv.janis.iom.repository.NotificationTaskRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationTaskProcessorTest {
  @Mock
  NotificationTaskRepository notificationTaskRepository;
  @Mock
  NotificationSender notificationSender;

  @InjectMocks
  NotificationTaskProcessor notificationTaskProcessor;

  @Test
  void processPendingTasks_noTasks_doesNothing() {
    when(notificationTaskRepository
        .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(NotificationTaskStatus.PENDING), any(Instant.class)))
        .thenReturn(List.of());

    notificationTaskProcessor.processPendingTasks();

    verifyNoInteractions(notificationSender);
  }

  @Test
  void processPendingTasks_success_marksSent() {
    var task = new NotificationTask(Inventory.createFor(product("SKU-1"), 5, 5, 10));
    when(notificationTaskRepository
        .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(NotificationTaskStatus.PENDING), any(Instant.class)))
        .thenReturn(List.of(task));

    var start = Instant.now();
    notificationTaskProcessor.processPendingTasks();

    assertEquals(NotificationTaskStatus.SENT, task.getStatus());
    assertEquals(0, task.getAttempts());
    assertNotNull(task.getNextAttemptAt());
    assertFalse(task.getNextAttemptAt().isBefore(start));
    verify(notificationSender).sendLowStockAlert(task.getInventory());
  }

  @Test
  void processPendingTasks_failure_retriesWithBackoff() {
    var task = new NotificationTask(Inventory.createFor(product("SKU-1"), 5, 5, 10));
    when(notificationTaskRepository
        .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            eq(NotificationTaskStatus.PENDING), any(Instant.class)))
        .thenReturn(List.of(task));
    doThrow(new RuntimeException("fail"))
        .when(notificationSender).sendLowStockAlert(task.getInventory());

    var start = Instant.now();
    notificationTaskProcessor.processPendingTasks();

    assertEquals(NotificationTaskStatus.PENDING, task.getStatus());
    assertEquals(1, task.getAttempts());
    assertNotNull(task.getNextAttemptAt());
    assertTrue(task.getNextAttemptAt().isAfter(start));
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }
}
