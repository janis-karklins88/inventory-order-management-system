package lv.janis.iom.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.iom.entity.Alert;
import lv.janis.iom.enums.AlertType;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByAlertTypeAndAcknowledgedAtIsNullOrderByCreatedAtDesc(AlertType type, Pageable pageable);

    Page<Alert> findByAcknowledgedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    Page<Alert> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since, Pageable pageable);


}
