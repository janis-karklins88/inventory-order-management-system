package lv.janis.iom.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.repository.InventoryRepository;
import lv.janis.iom.repository.ProductRepository;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
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

    private static void requireProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
    }

    private static void requireQuantity(Integer quantity, String paramName) {
        if (quantity == null) {
            throw new IllegalArgumentException(paramName + " is required");
        }
    }



}
