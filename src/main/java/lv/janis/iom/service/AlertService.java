package lv.janis.iom.service;

import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.lang.NonNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

import lv.janis.iom.entity.Alert;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.enums.AlertType;
import lv.janis.iom.repository.AlertRepository;

@Service
public class AlertService {
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert createLowStockAlert(Inventory inventory) {

        Alert alert = Alert.createLowStockAlert(inventory);
        return alertRepository.save(alert);
    }

    @Transactional
    public void acknowledgeAlert(@NonNull Long alertId) {
        var alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found with id: " + alertId));
        alert.acknowledge(Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<Alert> getAlerts(Boolean unacknowledgedOnly, AlertType type, @NonNull Pageable pageable) {
        if (Boolean.TRUE.equals(unacknowledgedOnly) && type != null) {
            return alertRepository.findByAlertTypeAndAcknowledgedAtIsNullOrderByCreatedAtDesc(type, pageable);
        }
        if (Boolean.TRUE.equals(unacknowledgedOnly)) {
            return alertRepository.findByAcknowledgedAtIsNullOrderByCreatedAtDesc(pageable);
        }

        return alertRepository.findAll(pageable);

    }

}
