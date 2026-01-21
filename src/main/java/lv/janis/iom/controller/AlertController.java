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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lv.janis.iom.enums.AlertType;
import lv.janis.iom.dto.response.AlertResponse;
import lv.janis.iom.service.AlertService;

@Tag(name = "Alerts", description = "Alert management endpoints")
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "List alerts")
    @ApiResponse(responseCode = "200", description = "Alerts listed")
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> listAlerts(
        @Parameter(description = "Only return unacknowledged alerts")
        @RequestParam(required = false) Boolean unacknowledgedOnly,
        @Parameter(description = "Filter by alert type")
        @RequestParam(required = false) AlertType type,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = alertService.getAlerts(unacknowledgedOnly, type, pageable)
            .map(AlertResponse::from);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Acknowledge alert")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Alert acknowledged"),
        @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
        @Parameter(description = "Alert id", example = "9001") @PathVariable Long alertId
    ) {
        alertService.acknowledgeAlert(alertId);
        return ResponseEntity.noContent().build();
    }
}
