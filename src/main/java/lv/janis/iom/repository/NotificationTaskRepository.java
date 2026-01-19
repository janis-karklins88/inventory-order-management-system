package lv.janis.iom.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import lv.janis.iom.entity.NotificationTask;
import lv.janis.iom.enums.NotificationTaskStatus;
import jakarta.persistence.LockModeType;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<NotificationTask> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        NotificationTaskStatus status, Instant now);

        
    
}
