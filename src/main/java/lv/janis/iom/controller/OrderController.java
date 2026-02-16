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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.lang.NonNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.CustomerOrderFilter;
import lv.janis.iom.dto.requests.ExternalOrderCancelRequest;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.requests.OrderItemAddRequest;
import lv.janis.iom.dto.requests.OrderReturnRequest;
import lv.janis.iom.dto.response.CustomerOrderResponse;
import lv.janis.iom.dto.response.ExternalOrderStatusResponse;
import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.facade.ExternalOrderFacade;

@Tag(name = "Orders", description = "Order management endpoints")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

        private final OrderService orderService;
        private final ExternalOrderFacade externalOrderFacade;

        public OrderController(OrderService orderService, ExternalOrderFacade externalOrderFacade) {
                this.orderService = orderService;
                this.externalOrderFacade = externalOrderFacade;
        }

        @Operation(summary = "Create order")
        @ApiResponse(responseCode = "201", description = "Order created")
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

        @Operation(summary = "Create external order", description = "Idempotent by (source, externalOrderId). Duplicate requests return the existing order.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "Order accepted for asynchronous processing"),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "One or more products not found")
        })
        @PostMapping("/external")
        public ResponseEntity<Void> createExternalOrder(
                        @Valid @RequestBody ExternalOrderIngestRequest request) {
                Long orderId = externalOrderFacade.ingest(request);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/orders/{id}")
                                .buildAndExpand(orderId)
                                .toUri();

                URI statusLocation = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/orders/external/status")
                                .queryParam("source", request.getSource())
                                .queryParam("externalOrderId", request.getExternalOrderId())
                                .build()
                                .toUri();

                return ResponseEntity.accepted()
                                .location(location)
                                .header("Link", "<" + statusLocation + ">; rel=\"status\"")
                                .build();
        }

        @Operation(summary = "Cancel external order", description = "Requests cancellation by (source, externalOrderId) and notifies source via webhook.")
        @ApiResponses({
                        @ApiResponse(responseCode = "202", description = "Cancellation request accepted", headers = {
                                        @Header(name = "Location", description = "Absolute URL of the internal order resource"),
                                        @Header(name = "Link", description = "Status endpoint for polling, for example </api/orders/external/status?source=WEB_SHOP&externalOrderId=EXT-9>; rel=\"status\"")
                        }),
                        @ApiResponse(responseCode = "404", description = "Order not found for (source, externalOrderId)"),
                        @ApiResponse(responseCode = "400", description = "Invalid request")
        })
        @PostMapping("/external/cancel")
        public ResponseEntity<Void> cancelExternalOrder(
                        @Valid @RequestBody ExternalOrderCancelRequest request) {
                Long orderId = externalOrderFacade.cancel(request);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/orders/{id}")
                                .buildAndExpand(orderId)
                                .toUri();

                URI statusLocation = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/orders/external/status")
                                .queryParam("source", request.getSource())
                                .queryParam("externalOrderId", request.getExternalOrderId())
                                .build()
                                .toUri();

                return ResponseEntity.accepted()
                                .location(location)
                                .header("Link", "<" + statusLocation + ">; rel=\"status\"")
                                .build();
        }

        @Operation(summary = "Add item to order")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Item added"),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "404", description = "Order or product not found"),
                        @ApiResponse(responseCode = "409", description = "Order is not in CREATED status")
        })
        @PostMapping("/{orderId}/items")
        public ResponseEntity<CustomerOrderResponse> addItem(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId,
                        @Valid @RequestBody OrderItemAddRequest request) {
                var order = orderService.addItem(orderId, request.getProductId(), request.getQuantity());
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Remove item from order")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Item removed"),
                        @ApiResponse(responseCode = "404", description = "Order or item not found"),
                        @ApiResponse(responseCode = "409", description = "Order is not in CREATED status")
        })
        @DeleteMapping("/{orderId}/items/{orderItemId}")
        public ResponseEntity<CustomerOrderResponse> removeItem(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId,
                        @Parameter(description = "Order item id", example = "501") @PathVariable @NonNull Long orderItemId) {
                var order = orderService.removeItem(orderId, orderItemId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Mark order processing")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Order updated"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "409", description = "Invalid order status or empty items")
        })
        @PostMapping("/{orderId}/processing")
        public ResponseEntity<CustomerOrderResponse> statusProcessing(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId) {
                var order = orderService.statusProcessing(orderId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Mark order shipped")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Order updated"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "409", description = "Invalid order status")
        })
        @PostMapping("/{orderId}/shipped")
        public ResponseEntity<CustomerOrderResponse> statusShipped(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId) {
                var order = orderService.statusShipped(orderId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Mark order delivered")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Order updated"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "409", description = "Invalid order status")
        })
        @PostMapping("/{orderId}/delivered")
        public ResponseEntity<CustomerOrderResponse> statusDelivered(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId) {
                var order = orderService.statusDelivered(orderId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Mark order cancelled")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Order updated"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "409", description = "Invalid order status")
        })
        @PostMapping("/{orderId}/cancelled")
        public ResponseEntity<CustomerOrderResponse> statusCancelled(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId) {
                var order = orderService.statusCancelled(orderId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Mark order returned")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Order updated"),
                        @ApiResponse(responseCode = "404", description = "Order not found"),
                        @ApiResponse(responseCode = "409", description = "Invalid order status")
        })
        @PostMapping("/{orderId}/returned")
        public ResponseEntity<CustomerOrderResponse> statusReturned(
                        @Parameter(description = "Order id", example = "1001") @PathVariable @NonNull Long orderId,
                        @RequestBody(required = false) OrderReturnRequest request) {
                var productIds = request != null ? request.getProductIds() : null;
                var order = orderService.statusReturned(orderId, productIds);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "Get order by id")
        @ApiResponse(responseCode = "200", description = "Order found")
        @GetMapping("/{orderId}")
        public ResponseEntity<CustomerOrderResponse> getCustomerOrderById(@PathVariable @NonNull Long orderId) {
                var order = orderService.getCustomerOrderById(orderId);
                return ResponseEntity.ok(CustomerOrderResponse.from(order));
        }

        @Operation(summary = "List orders")
        @ApiResponse(responseCode = "200", description = "Orders listed")
        @GetMapping
        public ResponseEntity<Page<CustomerOrderResponse>> listOrders(
                        @Parameter(description = "Filter options") @ParameterObject @ModelAttribute CustomerOrderFilter filter,
                        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
                var page = orderService.getCustomerOrders(filter, pageable);
                return ResponseEntity.ok(page);
        }

        @GetMapping("/external/status")
        public ResponseEntity<ExternalOrderStatusResponse> getExternalStatus(
                @RequestParam ExternalOrderSource source,
                @RequestParam String externalOrderId) {

                var order = orderService.findBySourceAndExternalOrderId(source, externalOrderId);

                return ResponseEntity.ok(ExternalOrderStatusResponse.from(order));
        }

}
