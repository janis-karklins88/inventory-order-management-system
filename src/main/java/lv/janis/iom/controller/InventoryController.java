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

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.InventoryFilter;
import lv.janis.iom.dto.requests.InventoryAdjustRequest;
import lv.janis.iom.dto.requests.InventoryCreationRequest;
import lv.janis.iom.dto.response.InventoryResponse;
import lv.janis.iom.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<InventoryResponse> createInventory(
        @PathVariable Long productId,
        @Valid @RequestBody InventoryCreationRequest request
    ) {
        var inventory = inventoryService.createInventory(productId, request);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        var inventory = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> listInventory(
        @ModelAttribute InventoryFilter filter,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = inventoryService.getInventory(filter, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/in-stock")
    public ResponseEntity<List<InventoryResponse>> listInStockInventory() {
        var items = inventoryService.listInStockAllInventory()
            .stream()
            .map(InventoryResponse::from)
            .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Integer> getAvailableStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getAvailableStock(productId));
    }

    @PostMapping("/{productId}/add")
    public ResponseEntity<InventoryResponse> addStock(
        @PathVariable Long productId,
        @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.addStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponse> reduceStock(
        @PathVariable Long productId,
        @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.reduceStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserveStock(
        @PathVariable Long productId,
        @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.reserveStock(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/reserve/cancel")
    public ResponseEntity<InventoryResponse> cancelReservedQuantity(
        @PathVariable Long productId,
        @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.cancelReservedQuantity(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/reserve/fulfill")
    public ResponseEntity<InventoryResponse> fulfillReservedQuantity(
        @PathVariable Long productId,
        @RequestParam Integer quantity
    ) {
        var inventory = inventoryService.fulfillReservedQuantity(productId, quantity);
        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<InventoryResponse> adjustInventoryQuantity(
        @PathVariable Long productId,
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
