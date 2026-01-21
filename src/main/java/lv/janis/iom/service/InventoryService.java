package lv.janis.iom.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.filters.InventoryFilter;
import lv.janis.iom.dto.requests.InventoryCreationRequest;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.dto.response.InventoryResponse;
import lv.janis.iom.entity.Alert;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.NotificationTask;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.repository.AlertRepository;
import lv.janis.iom.repository.InventoryRepository;
import lv.janis.iom.repository.NotificationTaskRepository;
import lv.janis.iom.repository.ProductRepository;
import lv.janis.iom.repository.specification.InventorySpecifications;

@Service
@Transactional
public class InventoryService {

    private final AlertRepository alertRepository;

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final NotificationTaskRepository notificationTaskRepository;


    public InventoryService(
        InventoryRepository inventoryRepository, 
        ProductRepository productRepository, 
        StockMovementService stockMovementService, 
        NotificationTaskRepository notificationTaskRepository
    , AlertRepository alertRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
        this.notificationTaskRepository = notificationTaskRepository;
        this.alertRepository = alertRepository;
    }

    public Inventory createInventory(Long productId, InventoryCreationRequest request) {
        requireProductId(productId);
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        requireNonNegative(request.getQuantity(), "quantity");
        requireNonNegative(request.getReorderLevel(), "reorderLevel");
        int clearLowQuantity = Optional.ofNullable(request.getClearLowQuantity()).orElse(0);
        requireNonNegative(clearLowQuantity, "clearLowQuantity");

        return inventoryRepository.findByProductId(productId)
            .orElseGet(() -> {
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product with id " + productId + " not found"));
                try {
                    Inventory inventory = Inventory.createFor(
                        product,
                        request.getQuantity(),
                        request.getReorderLevel(),
                        clearLowQuantity
                    );
                    updateLowQuantityFlag(inventory);
                    return inventoryRepository.saveAndFlush(inventory);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    return inventoryRepository.findByProductId(productId)
                        .orElseThrow(() -> e);
                }
            });
    }

    public Inventory getInventoryByProductId(Long productId) {
        requireProductId(productId);
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
    }

    public Inventory addStock(Long productId, Integer quantityToAdd) {
        requireProductId(productId);
        requireQuantity(quantityToAdd, "quantityToAdd");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        inventory.increaseQuantity(quantityToAdd);
        updateLowQuantityFlag(inventory);
        return inventoryRepository.save(inventory);
    }

    public Inventory reduceStock(Long productId, Integer quantityToReduce) {
        requireProductId(productId);
        requireQuantity(quantityToReduce, "quantityToReduce");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        boolean wasLowStock = inventory.isLowQuantity();
        inventory.decreaseQuantity(quantityToReduce);
        updateLowQuantityFlag(inventory);
        lowStockCheck(inventory, wasLowStock);
        return inventoryRepository.save(inventory);
    }

    public Inventory reserveStock(Long productId, Integer quantityToReserve) {
        requireProductId(productId);
        requireQuantity(quantityToReserve, "quantityToReserve");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        boolean wasLowStock = inventory.isLowQuantity();
        inventory.reserveQuantity(quantityToReserve);
        updateLowQuantityFlag(inventory);
        lowStockCheck(inventory, wasLowStock);
        return inventoryRepository.save(inventory);
    }

    public Inventory cancelReservedQuantity(Long productId, Integer quantityToCancel) {
        requireProductId(productId);
        requireQuantity(quantityToCancel, "quantityToCancel");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        inventory.unreserveQuantity(quantityToCancel);
        updateLowQuantityFlag(inventory);
        return inventoryRepository.save(inventory);
    }

    public Inventory fulfillReservedQuantity(Long productId, Integer quantityToReduce) {
        requireProductId(productId);
        requireQuantity(quantityToReduce, "quantityToReduce");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        boolean wasLowStock = inventory.isLowQuantity();
        inventory.deductReservedQuantity(quantityToReduce);
        updateLowQuantityFlag(inventory);
        lowStockCheck(inventory, wasLowStock);
        return inventoryRepository.save(inventory);
    }


    public int getAvailableStock(Long productId) {
        requireProductId(productId);
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));
        return inventory.getAvailableQuantity();
    }

    public Page<InventoryResponse> getInventory(InventoryFilter filter, Pageable pageable) {
        var safePageable = capPageSize(pageable, 100);
        var safeFilter = filter != null ? filter : new InventoryFilter();
        var spec = Specification.where(
            InventorySpecifications.search(safeFilter.getQ())
                .and(InventorySpecifications.quantityGte(safeFilter.getMinQuantity()))
                .and(InventorySpecifications.quantityLte(safeFilter.getMaxQuantity()))
                .and(InventorySpecifications.availableGte(safeFilter.getMinAvailable()))
                .and(InventorySpecifications.availableLte(safeFilter.getMaxAvailable()))
                .and(InventorySpecifications.stockStatus(safeFilter.getStockStatus()))
                .and(InventorySpecifications.productNotDeleted())
        );
        return inventoryRepository.findAll(spec, safePageable).map(InventoryResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Inventory> listInStockAllInventory() {
    return inventoryRepository.findAllInStockWithProduct();
    }

    public Inventory adjustInventoryQuantity(Long productId, Integer delta, String reason) {
        requireProductId(productId);
        if (delta == null) {
            throw new IllegalArgumentException("delta is required");
        }
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product id " + productId + " not found"));

        if(reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if(delta > 0) {
            inventory.increaseQuantity(delta);
            updateLowQuantityFlag(inventory);
            stockMovementService.createStockMovement(
                new StockMovementCreationRequest(
                    inventory,
                    MovementType.MANUAL_ADJUSTMENT,
                    delta,
                    reason,
                    null
                )
            );
        }
        else if(delta < 0) {
            int absDelta = Math.abs(delta);
            boolean wasLowStock = inventory.isLowQuantity();
            inventory.decreaseQuantity(absDelta);
            updateLowQuantityFlag(inventory);
            lowStockCheck(inventory, wasLowStock);
            stockMovementService.createStockMovement(
                new StockMovementCreationRequest(
                    inventory,
                    MovementType.MANUAL_ADJUSTMENT,
                    delta,
                    reason,
                    null
                )
            );
        }
        else {
            throw new IllegalArgumentException("delta cannot be zero");
        }
        return inventoryRepository.save(inventory);
    }

    public void updateLowQuantityFlag(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory is required");
        }
        int available = inventory.getAvailableQuantity();
        if (inventory.isLowQuantity()) {
            if (available > inventory.getClearLowQuantity()) {
                inventory.setIsLowQuantity(false);
            }
        } else if (available <= inventory.getReorderLevel()) {
            inventory.setIsLowQuantity(true);
        }
    }

    public void lowStockCheck(Inventory inventory, boolean wasLowStock) {
        boolean isLowStockNow = inventory.isLowQuantity();
        if (!wasLowStock && isLowStockNow) {
            var newTask = new NotificationTask(inventory);
            var newAlert = Alert.createLowStockAlert(inventory);
            notificationTaskRepository.save(newTask);
            alertRepository.save(newAlert);
        }
    }

    private static void requireProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
    }

    private static void requireQuantity(Integer quantity, String paramName) {
        if (quantity == null) {
            throw new IllegalArgumentException(paramName + " is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive");
        }
    }

    private static void requireNonNegative(Integer value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " is required");
        }
        if (value < 0) {
            throw new IllegalArgumentException(paramName + " must be zero or positive");
        }
    }

    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            return PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
        }
        return pageable;
    }



}
