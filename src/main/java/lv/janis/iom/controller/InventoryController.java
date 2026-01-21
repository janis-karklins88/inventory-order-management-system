package lv.janis.iom.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.InventoryFilter;
import lv.janis.iom.dto.requests.InventoryAdjustRequest;
import lv.janis.iom.dto.requests.InventoryCreationRequest;
import lv.janis.iom.dto.response.InventoryResponse;
import lv.janis.iom.service.InventoryService;

@Tag(name = "Inventory", description = "Inventory management endpoints")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(
        summary = "Create inventory entry",
        description = "Creates the initial inventory record for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Inventory already exists")
    })
    @PostMapping("/{productId}")
    public ResponseEntity<InventoryResponse> createInventory(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Valid @RequestBody InventoryCreationRequest request
    ) {
        var inventory = inventoryService.createInventory(productId, request);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(summary = "Get inventory by product id")
    @ApiResponse(responseCode = "200", description = "Inventory found")
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        var inventory = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(summary = "List inventory")
    @ApiResponse(responseCode = "200", description = "Inventory listed")
    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> listInventory(
        @Parameter(description = "Filter options") @ParameterObject
        @ModelAttribute InventoryFilter filter,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = inventoryService.getInventory(filter, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "List in-stock inventory")
    @ApiResponse(responseCode = "200", description = "In-stock inventory listed")
    @GetMapping("/in-stock")
    public ResponseEntity<List<InventoryResponse>> listInStockInventory() {
        var items = inventoryService.listInStockAllInventory()
            .stream()
            .map(InventoryResponse::from)
            .toList();
        return ResponseEntity.ok(items);
    }

    @Operation(summary = "Get available stock by product id")
    @ApiResponse(responseCode = "200", description = "Available stock returned")
    @GetMapping("/{productId}/available")
    public ResponseEntity<Integer> getAvailableStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getAvailableStock(productId));
    }

    @Operation(
        summary = "Add stock to inventory",
        description = "Increases on-hand quantity for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock added"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found")
    })
    @PostMapping("/{productId}/add")
    public ResponseEntity<InventoryResponse> addStock(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Parameter(description = "Quantity to add", example = "10") @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.addStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(
        summary = "Reduce stock from inventory",
        description = "Decreases on-hand quantity for a product."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reduced"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient available stock")
    })
    @PostMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponse> reduceStock(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Parameter(description = "Quantity to reduce", example = "5") @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.reduceStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(
        summary = "Reserve stock",
        description = "Reserves available stock for an order."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock reserved"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient available stock")
    })
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Parameter(description = "Quantity to reserve", example = "3") @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(
        summary = "Cancel reserved stock",
        description = "Releases a reserved quantity back to available stock."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserved stock cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found"),
        @ApiResponse(responseCode = "409", description = "Reservation not sufficient")
    })
    @PostMapping("/{productId}/reserve/cancel")
    public ResponseEntity<InventoryResponse> cancelReservedQuantity(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Parameter(description = "Quantity to cancel", example = "2") @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.cancelReservedQuantity(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(
        summary = "Fulfill reserved stock",
        description = "Consumes a reserved quantity when an order ships."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserved stock fulfilled"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found"),
        @ApiResponse(responseCode = "409", description = "Reservation not sufficient")
    })
    @PostMapping("/{productId}/reserve/fulfill")
    public ResponseEntity<InventoryResponse> fulfillReservedQuantity(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Parameter(description = "Quantity to fulfill", example = "2") @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.fulfillReservedQuantity(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @Operation(
        summary = "Adjust inventory quantity",
        description = "Applies a signed quantity delta with a reason."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory adjusted"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Inventory or product not found")
    })
    @PostMapping("/{productId}/adjust")
    public ResponseEntity<InventoryResponse> adjustInventoryQuantity(
        @Parameter(description = "Product id", example = "42") @PathVariable Long productId,
        @Valid @RequestBody InventoryAdjustRequest request
    ) {
        var inventory = inventoryService.adjustInventoryQuantity(
            productId,
            request.getDelta(),
            request.getReason()
        );
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }
}
