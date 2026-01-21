package lv.janis.iom.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lv.janis.iom.enums.AlertType;
import lv.janis.iom.dto.response.AlertResponse;
import lv.janis.iom.service.AlertService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<Page<AlertResponse>> listAlerts(
        @RequestParam(required = false) Boolean unacknowledgedOnly,
        @RequestParam(required = false) AlertType type,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = alertService.getAlerts(unacknowledgedOnly, type, pageable)
            .map(AlertResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable Long alertId) {
        alertService.acknowledgeAlert(alertId);
        return ResponseEntity.noContent().build();
    }
}
