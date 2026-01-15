package lv.janis.iom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.iom.dto.StockMovementCreationRequest;
import lv.janis.iom.dto.filters.InventoryFilter;
import lv.janis.iom.dto.response.InventoryResponse;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.repository.InventoryRepository;
import lv.janis.iom.repository.ProductRepository;
import lv.janis.iom.repository.specification.InventorySpecifications;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository, StockMovementService stockMovementService) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
    }

    public Inventory createInventory(Long productId) {
        requireProductId(productId);

        return inventoryRepository.findByProductId(productId)
            .orElseGet(() -> {
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product with id " + productId + " not found"));
                try {
                    return inventoryRepository.saveAndFlush(Inventory.createFor(product));
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    return inventoryRepository.findByProductId(productId)
                        .orElseThrow(() -> e);
                }
            });
    }

    public Inventory getInventoryByProductId(Long productId) {
        requireProductId(productId);
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
    }

    public Inventory addStock(Long productId, Integer quantityToAdd) {
        requireProductId(productId);
        requireQuantity(quantityToAdd, "quantityToAdd");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        inventory.increaseQuantity(quantityToAdd);
        return inventoryRepository.save(inventory);
    }

    public Inventory reduceStock(Long productId, Integer quantityToReduce) {
        requireProductId(productId);
        requireQuantity(quantityToReduce, "quantityToReduce");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        inventory.decreaseQuantity(quantityToReduce);
        return inventoryRepository.save(inventory);
    }

    public Inventory reserveStock(Long productId, Integer quantityToReserve) {
        requireProductId(productId);
        requireQuantity(quantityToReserve, "quantityToReserve");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        inventory.reserveQuantity(quantityToReserve);
        return inventoryRepository.save(inventory);
    }

    public Inventory cancelReservedQuantity(Long productId, Integer quantityToCancel) {
        requireProductId(productId);
        requireQuantity(quantityToCancel, "quantityToCancel");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        inventory.unreserveQuantity(quantityToCancel);
        return inventoryRepository.save(inventory);
    }

    public Inventory fulfillReservedQuantity(Long productId, Integer quantityToReduce) {
        requireProductId(productId);
        requireQuantity(quantityToReduce, "quantityToReduce");
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        inventory.deductReservedQuantity(quantityToReduce);
        return inventoryRepository.save(inventory);
    }


    public int getAvailableStock(Long productId) {
        requireProductId(productId);
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
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
        );
        return inventoryRepository.findAll(spec, safePageable).map(InventoryResponse::from);
    }

    public Inventory adjustInventoryQuantity(Long productId, Integer delta, String reason) {
        requireProductId(productId);
        if (delta == null) {
            throw new IllegalArgumentException("delta is required");
        }
        var inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new IllegalArgumentException("Inventory for product id " + productId + " not found"));
        if(reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if(delta > 0) {
            inventory.increaseQuantity(delta);
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
            inventory.decreaseQuantity(absDelta);
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

    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            return PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
        }
        return pageable;
    }



}
