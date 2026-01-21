package lv.janis.iom.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import lv.janis.iom.dto.filters.StockMovmentFilter;
import lv.janis.iom.dto.response.StockMovementResponse;
import lv.janis.iom.service.StockMovementService;

@Tag(name = "Stock Movements", description = "Stock movement endpoints")
@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @Operation(
        summary = "List stock movements",
        description = "Filter by productId, inventoryId, orderId, movementType, direction, and time range (from/to)."
    )
    @ApiResponse(responseCode = "200", description = "Stock movements listed")
    @GetMapping
    public ResponseEntity<Page<StockMovementResponse>> listStockMovements(
        @Parameter(description = "Filter options") @ParameterObject
        @ModelAttribute StockMovmentFilter filter,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = stockMovementService.getStockMovement(filter, pageable);
        return ResponseEntity.ok(page);
    }
}
