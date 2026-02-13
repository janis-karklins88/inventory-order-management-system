package lv.janis.iom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import lv.janis.iom.dto.filters.CustomerOrderFilter;
import lv.janis.iom.dto.response.CustomerOrderResponse;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.enums.FailureCode;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.factory.StockMovementRequestFactory;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.OutboxEventRepository;
import lv.janis.iom.repository.ProductRepository;
import lv.janis.iom.repository.specification.OrderSpecifications;
import org.springframework.lang.NonNull;

import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final OutboxEventRepository outboxRepo;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            InventoryService inventoryService,
            StockMovementService stockMovementService,
            OutboxEventRepository outboxRepo

    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.stockMovementService = stockMovementService;
        this.outboxRepo = outboxRepo;

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
        return statusReturned(orderId, null);
    }

    @Transactional
    public CustomerOrder statusReturned(@NonNull Long orderId, List<Long> productIds) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only orders in DELIVERED status can be moved to RETURNED");
        }

        Set<Long> requestedProductIds = new HashSet<>();
        if (productIds != null) {
            for (Long productId : productIds) {
                requireId(productId, "productId");
                requestedProductIds.add(productId);
            }
        }

        List<OrderItem> itemsToReturn;
        if (requestedProductIds.isEmpty()) {
            itemsToReturn = new ArrayList<>(order.getItems());
        } else {
            itemsToReturn = order.getItems().stream()
                    .filter(item -> requestedProductIds.contains(item.getProduct().getId()))
                    .toList();

            Set<Long> foundProductIds = itemsToReturn.stream()
                    .map(item -> item.getProduct().getId())
                    .collect(java.util.stream.Collectors.toSet());
            requestedProductIds.removeAll(foundProductIds);
            if (!requestedProductIds.isEmpty()) {
                throw new EntityNotFoundException("Products not found in order: " + requestedProductIds);
            }
        }

        for (var item : itemsToReturn) {
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
    public CustomerOrder findBySourceAndExternalOrderId(@NonNull ExternalOrderSource source,
            @NonNull String externalOrderId) {
        if (externalOrderId.isBlank()) {
            throw new IllegalArgumentException("externalOrderId is required");
        }
        return customerOrderRepository.findBySourceAndExternalOrderId(source, externalOrderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order with source " + source + " and externalOrderId " + externalOrderId + " not found"));
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

    @Transactional
    public void markRejected(Long orderId,
            FailureCode code,
            String message) {

        var order = getCustomerOrderById(orderId);

        order.setStatus(OrderStatus.REJECTED);
        order.setFailureCode(code);
        order.setFailureMessage(message);
        order.setFailedAt(Instant.now());

        customerOrderRepository.save(order);
        outboxRepo.save(OutboxEvent.pending(
                OutboxEventType.EXTERNAL_ORDER_REJECTED,
                order.getId(),
                "{\"orderId\":" + order.getId() + "}"));
    }

    @Transactional
    public void markFailed(Long orderId,
            FailureCode code,
            String message) {

        var order = getCustomerOrderById(orderId);

        order.setStatus(OrderStatus.FAILED);
        order.setFailureCode(code);
        order.setFailureMessage(message);
        order.setFailedAt(Instant.now());
        order.incrementRetryCount();

        customerOrderRepository.save(order);
    }

}
