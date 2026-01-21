package lv.janis.iom.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.CustomerOrderFilter;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.requests.OrderItemAddRequest;
import lv.janis.iom.dto.response.CustomerOrderResponse;
import lv.janis.iom.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CustomerOrderResponse> createOrder() {
        var order = orderService.createOrder();
        var response = CustomerOrderResponse.from(order);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(order.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/external")
    public ResponseEntity<CustomerOrderResponse> createExternalOrder(
        @Valid @RequestBody ExternalOrderIngestRequest request
    ) {
        var order = orderService.createExternalOrder(request);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<CustomerOrderResponse> addItem(
        @PathVariable Long orderId,
        @Valid @RequestBody OrderItemAddRequest request
    ) {
        var order = orderService.addItem(orderId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<CustomerOrderResponse> removeItem(
        @PathVariable Long orderId,
        @PathVariable Long orderItemId
    ) {
        var order = orderService.removeItem(orderId, orderItemId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/processing")
    public ResponseEntity<CustomerOrderResponse> statusProcessing(@PathVariable Long orderId) {
        var order = orderService.statusProcessing(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/shipped")
    public ResponseEntity<CustomerOrderResponse> statusShipped(@PathVariable Long orderId) {
        var order = orderService.statusShipped(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/delivered")
    public ResponseEntity<CustomerOrderResponse> statusDelivered(@PathVariable Long orderId) {
        var order = orderService.statusDelivered(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/cancelled")
    public ResponseEntity<CustomerOrderResponse> statusCancelled(@PathVariable Long orderId) {
        var order = orderService.statusCancelled(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/returned")
    public ResponseEntity<CustomerOrderResponse> statusReturned(@PathVariable Long orderId) {
        var order = orderService.statusReturned(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<CustomerOrderResponse> getCustomerOrderById(@PathVariable Long orderId) {
        var order = orderService.getCustomerOrderById(orderId);
        return ResponseEntity.ok(CustomerOrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerOrderResponse>> listOrders(
        @ModelAttribute CustomerOrderFilter filter,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = orderService.getCustomerOrders(filter, pageable);
        return ResponseEntity.ok(page);
    }
}
