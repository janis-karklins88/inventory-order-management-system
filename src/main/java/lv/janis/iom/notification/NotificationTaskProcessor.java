package lv.janis.iom.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import lv.janis.iom.enums.NotificationTaskStatus;
import lv.janis.iom.repository.NotificationTaskRepository;

@Component
public class NotificationTaskProcessor {
    private static final Logger log = LoggerFactory.getLogger(NotificationTaskProcessor.class);
    private final NotificationTaskRepository notificationTaskRepository;
    private final NotificationSender notificationSender;

    public NotificationTaskProcessor(
        NotificationTaskRepository notificationTaskRepository,
        NotificationSender notificationSender) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.notificationSender = notificationSender;
    }

    @Scheduled(fixedDelayString = "${notification.task.processor.delay-ms:5000}")
    @Transactional
    public void processPendingTasks() {
        var now = java.time.Instant.now();
        var tasks = notificationTaskRepository
            .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                NotificationTaskStatus.PENDING, now);

        for (var task : tasks) {
            task.setStatus(NotificationTaskStatus.PROCESSING);
            try {
                notificationSender.sendLowStockAlert(task.getInventory());
                task.setStatus(NotificationTaskStatus.SENT);
                task.setNextAttemptAt(now);
            } catch (Exception ex) {
                task.incrementAttempts();
                task.setStatus(NotificationTaskStatus.PENDING);
                var delaySeconds = calculateBackoffSeconds(task.getAttempts());
                task.setNextAttemptAt(now.plusSeconds(delaySeconds));
                log.warn("Failed to send low stock alert for taskId={}, attempt={}, retryInSeconds={}",
                    task.getId(), task.getAttempts(), delaySeconds, ex);
            }
        }
    }

    private static long calculateBackoffSeconds(int attempts) {
        int cappedAttempts = Math.min(attempts, 6);
        long delay = (long) Math.pow(2, cappedAttempts);
        return Math.min(delay, 3600);
    }
}
