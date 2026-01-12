package lv.janis.iom.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.ProductRepository;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;


    public OrderService(
        CustomerOrderRepository customerOrderRepository,
        ProductRepository productRepository,
        InventoryService inventoryService

    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;

    }

    @Transactional
    public CustomerOrder createOrder(){
        CustomerOrder order = CustomerOrder.create();
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder addItem(Long orderId, Long productId, int quantity) {
        requireId(orderId, "orderId");
        requireId(productId, "productId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));

        var product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product with id " + productId + " not found"));

        var priceAtOrderTime = product.getPrice();
        var orderItem = OrderItem.createFor(product, quantity, priceAtOrderTime);
        order.addItem(orderItem);
        return order;
    }

    @Transactional
    public CustomerOrder removeItem(Long orderId, Long orderItemId) {
        requireId(orderId, "orderId");
        requireId(orderItemId, "orderItemId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));

        var orderItem = order.getItems().stream()
            .filter(item -> item.getId().equals(orderItemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Order item with id " + orderItemId + " not found"));

        order.removeItem(orderItem);
        return order;
    }



    @Transactional
    public CustomerOrder statusProcessing(Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Only orders in CREATED status can be moved to PROCESSING");
        }
        if(order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot process an order with no items");
        }

        
        for (var item : order.getItems()) {
            inventoryService.reserveStock(item.getProduct().getId(), item.getQuantity());
            }
            order.markProcessing();
            return order;
    }

        @Transactional
        public CustomerOrder statusShipped(Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Only orders in PROCESSING status can be moved to SHIPPED");
        }

        for (var item : order.getItems()) {
            inventoryService.fulfillReservedQuantity(item.getProduct().getId(), item.getQuantity());
        }
        order.markShipped();
        return order;
    }

    @Transactional
    public CustomerOrder statusDelivered(Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only orders in SHIPPED status can be moved to DELIVERED");
        }

        order.markDelivered();
        return customerOrderRepository.save(order);
    }

    @Transactional
    public CustomerOrder statusCancelled(Long orderId) {
        requireId(orderId, "orderId");
        var order = customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already CANCELLED");
        }

        if(order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that is SHIPPED or DELIVERED");
        }
        if (order.getStatus() == OrderStatus.PROCESSING) {
            for (var item : order.getItems()) {
                inventoryService.cancelReservedQuantity(item.getProduct().getId(), item.getQuantity());
            }
        }
        order.markCancelled();
        return order;
    }

    @Transactional
    public CustomerOrder getCustomerOrderById(Long orderId) {
        requireId(orderId, "orderId");
        return customerOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order with id " + orderId + " not found"));
    }

    private static void requireId(Long id, String name) {
        if (id == null) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    }
