package lv.janis.iom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.iom.dto.filters.CustomerOrderFilter;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.response.CustomerOrderResponse;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.factory.StockMovementRequestFactory;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.ProductRepository;
import lv.janis.iom.repository.specification.OrderSpecifications;
import org.springframework.lang.NonNull;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final EntityManager entityManager;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            InventoryService inventoryService,
            StockMovementService stockMovementService,
            EntityManager entityManager

    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.stockMovementService = stockMovementService;
        this.entityManager = entityManager;

    }

    @Transactional
    public CustomerOrder createOrder() {
        CustomerOrder order = CustomerOrder.create();
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder addItem(@NonNull Long orderId, Long productId, int quantity) {
        requireId(orderId, "orderId");
        requireId(productId, "productId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        var product = productRepository.findAllByIdInAndIsDeletedFalse(Set.of(productId))
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new EntityNotFoundException("Product with id " + productId + " not found or deleted"));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only modify items in CREATED");
        }

        var priceAtOrderTime = product.getPrice();
        var orderItem = OrderItem.createFor(product, quantity, priceAtOrderTime);
        order.addItem(orderItem);
        return order;
    }

    @Transactional
    public CustomerOrder removeItem(@NonNull Long orderId, Long orderItemId) {
        requireId(orderId, "orderId");
        requireId(orderItemId, "orderItemId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Can only modify items in CREATED");
        }

        var orderItem = order.getItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Order item with id " + orderItemId + " not found"));

        order.removeItem(orderItem);
        return order;
    }

    @Transactional
    public CustomerOrder statusProcessing(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only orders in CREATED status can be moved to PROCESSING");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot process an order with no items");
        }

        for (var item : order.getItems()) {
            var inventory = inventoryService.reserveStock(item.getProduct().getId(), item.getQuantity());

            stockMovementService.createStockMovement(
                    StockMovementRequestFactory.orderReserved(inventory, orderId, item.getQuantity()));

        }
        order.markProcessing();
        return order;
    }

    @Transactional
    public CustomerOrder statusShipped(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only orders in PROCESSING status can be moved to SHIPPED");
        }

        for (var item : order.getItems()) {
            inventoryService.fulfillReservedQuantity(item.getProduct().getId(), item.getQuantity());
            var inventory = inventoryService.getInventoryByProductId(item.getProduct().getId());
            stockMovementService.createStockMovement(
                    StockMovementRequestFactory.orderFulfilled(inventory, orderId, item.getQuantity()));
        }
        order.markShipped();
        return order;
    }

    @Transactional
    public CustomerOrder statusDelivered(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only orders in SHIPPED status can be moved to DELIVERED");
        }

        order.markDelivered();
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder statusCancelled(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already CANCELLED");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that is SHIPPED or DELIVERED");
        }
        if (order.getStatus() == OrderStatus.PROCESSING) {
            for (var item : order.getItems()) {
                inventoryService.cancelReservedQuantity(item.getProduct().getId(), item.getQuantity());
                var inventory = inventoryService.getInventoryByProductId(item.getProduct().getId());
                stockMovementService.createStockMovement(
                        StockMovementRequestFactory.orderReleased(inventory, orderId, item.getQuantity()));
                inventoryService.updateLowQuantityFlag(inventory);
            }
        }
        order.markCancelled();
        return order;
    }

    @Transactional
    public CustomerOrder statusReturned(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only orders in DELIVERED status can be moved to RETURNED");
        }

        for (var item : order.getItems()) {
            inventoryService.addStock(item.getProduct().getId(), item.getQuantity());
            var inventory = inventoryService.getInventoryByProductId(item.getProduct().getId());
            stockMovementService.createStockMovement(
                    StockMovementRequestFactory.orderReturned(inventory, orderId, item.getQuantity()));
        }
        order.markReturned();
        return order;
    }

    @Transactional
    public CustomerOrder getCustomerOrderById(@NonNull Long orderId) {
        requireId(orderId, "orderId");
        return customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderResponse> getCustomerOrders(CustomerOrderFilter filter, @NonNull Pageable pageable) {
        var safeFilter = filter != null ? filter : new CustomerOrderFilter();
        var safePageable = capPageSize(pageable, 100);
        var specs = Specification.where(
                OrderSpecifications.orderStatusEquals(safeFilter.getStatus())
                        .and(OrderSpecifications.createdBetween(safeFilter.getCreatedAfter(),
                                safeFilter.getCreatedBefore()))
                        .and(OrderSpecifications.updatedBetween(safeFilter.getUpdatedAfter(),
                                safeFilter.getUpdatedBefore())));

        return customerOrderRepository.findAll(specs, safePageable).map(CustomerOrderResponse::from);
    }

    @Transactional
    public CustomerOrder createExternalOrder(ExternalOrderIngestRequest request) {
        var existingOrder = customerOrderRepository.findBySourceAndExternalOrderId(
                request.getSource(),
                request.getExternalOrderId());
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }
        var order = CustomerOrder.create();
        order.setSource(request.getSource());
        order.setExternalOrderId(request.getExternalOrderId());
        order.setShippingAddress(request.getShippingAddress());
        // Sum quantities by product ID.
        var itemRequests = request.getItems();
        Map<Long, Integer> quantitiesByProductId = new HashMap<>();
        for (var itemReq : itemRequests) {
            quantitiesByProductId.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
        }
        Set<Long> productIds = quantitiesByProductId.keySet();
        // Load products in bulk and ensure none are missing or deleted.
        Map<Long, Product> productsById = new HashMap<>();
        for (var product : productRepository.findAllByIdInAndIsDeletedFalse(productIds)) {
            productsById.put(product.getId(), product);
        }
        if (productsById.size() != productIds.size()) {
            var missingIds = new HashSet<>(productIds);
            missingIds.removeAll(productsById.keySet());
            throw new EntityNotFoundException("Products not found or deleted: " + missingIds);
        }
        // Build order items with the current price snapshot.
        for (var entry : quantitiesByProductId.entrySet()) {
            var product = productsById.get(entry.getKey());
            var orderItem = OrderItem.createFor(product, entry.getValue(), product.getPrice());
            order.addItem(orderItem);
        }
        // Save and rely on the unique constraint for idempotency.
        try {
            customerOrderRepository.saveAndFlush(order);
        } catch (DataIntegrityViolationException ex) {
            // Clear failed entity state to avoid flush errors after constraint violations.
            entityManager.clear();
            // If it already exists, return the existing order instead of failing.
            return customerOrderRepository.findBySourceAndExternalOrderId(
                    request.getSource(),
                    request.getExternalOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Order from source " + request.getSource() + " with external ID "
                                    + request.getExternalOrderId() + " already exists",
                            ex));
        }
        return statusProcessing(
                Objects.requireNonNull(order.getId(), "Order ID must be present after save"));
    }

    private static void requireId(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    private @NonNull Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            return PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
        }
        return pageable;
    }

}
