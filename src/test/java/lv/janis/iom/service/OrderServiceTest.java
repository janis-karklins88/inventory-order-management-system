package lv.janis.iom.service;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.requests.ExternalOrderItemRequest;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.dto.response.CustomerOrderResponse;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.ProductRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
  @Mock
  CustomerOrderRepository customerOrderRepository;
  @Mock
  ProductRepository productRepository;
  @Mock
  InventoryService inventoryService;
  @Mock
  StockMovementService stockMovementService;

  @InjectMocks
  OrderService orderService;

  @Test
  void createOrder_saves() {
    when(customerOrderRepository.save(any(CustomerOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = orderService.createOrder();

    assertNotNull(result);
    assertEquals(OrderStatus.CREATED, result.getStatus());
    verify(customerOrderRepository).save(any(CustomerOrder.class));
  }

  @Test
  void addItem_missingOrder_throws() {
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.addItem(1L, 2L, 1));

    assertEquals("Order with id 1 not found", ex.getMessage());
  }

  @Test
  void addItem_missingProduct_throws() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(2L))).thenReturn(List.of());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.addItem(1L, 2L, 1));

    assertEquals("Product with id 2 not found or deleted", ex.getMessage());
  }

  @Test
  void addItem_orderNotCreated_throws() {
    var order = CustomerOrder.create();
    order.markProcessing();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(2L)))
        .thenReturn(List.of(product(2L, "SKU-2", new BigDecimal("9.99"))));

    var ex = assertThrows(IllegalStateException.class,
        () -> orderService.addItem(1L, 2L, 1));

    assertEquals("Can only modify items in CREATED", ex.getMessage());
  }

  @Test
  void addItem_ok_addsItemAndUpdatesTotal() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(2L)))
        .thenReturn(List.of(product));

    var result = orderService.addItem(1L, 2L, 2);

    assertSame(order, result);
    assertEquals(1, result.getItems().size());
    assertEquals(new BigDecimal("19.98"), result.getTotalAmount());
  }

  @Test
  void removeItem_missingOrder_throws() {
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.removeItem(1L, 10L));

    assertEquals("Order with id 1 not found", ex.getMessage());
  }

  @Test
  void removeItem_missingItem_throws() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.removeItem(1L, 10L));

    assertEquals("Order item with id 10 not found", ex.getMessage());
  }

  @Test
  void removeItem_ok_removesItem() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    var item = OrderItem.createFor(product, 2, product.getPrice());
    setId(item, 10L);
    order.addItem(item);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    var result = orderService.removeItem(1L, 10L);

    assertSame(order, result);
    assertEquals(0, result.getItems().size());
    assertEquals(BigDecimal.ZERO, result.getTotalAmount());
  }

  @Test
  void statusProcessing_noItems_throws() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    var ex = assertThrows(IllegalStateException.class,
        () -> orderService.statusProcessing(1L));

    assertEquals("Cannot process an order with no items", ex.getMessage());
  }

  @Test
  void statusProcessing_ok_reserves_andCreatesMovements() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    order.addItem(OrderItem.createFor(product, 2, product.getPrice()));
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(inventoryService.reserveStock(2L, 2))
        .thenReturn(Inventory.createFor(product, 10, 1, 2));

    var result = orderService.statusProcessing(1L);

    assertSame(order, result);
    assertEquals(OrderStatus.PROCESSING, result.getStatus());
    verify(inventoryService).reserveStock(2L, 2);
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
  }

  @Test
  void statusShipped_wrongStatus_throws() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

    var ex = assertThrows(IllegalStateException.class,
        () -> orderService.statusShipped(1L));

    assertEquals("Only orders in PROCESSING status can be moved to SHIPPED", ex.getMessage());
  }

  @Test
  void statusShipped_ok_fulfills_andCreatesMovements() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    order.addItem(OrderItem.createFor(product, 2, product.getPrice()));
    order.markProcessing();
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(inventoryService.getInventoryByProductId(2L))
        .thenReturn(Inventory.createFor(product, 10, 1, 2));

    var result = orderService.statusShipped(1L);

    assertSame(order, result);
    assertEquals(OrderStatus.SHIPPED, result.getStatus());
    verify(inventoryService).fulfillReservedQuantity(2L, 2);
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
  }

  @Test
  void statusDelivered_ok_marksAndSaves() {
    var order = CustomerOrder.create();
    order.markProcessing();
    order.markShipped();
    setId(order, 1L);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = orderService.statusDelivered(1L);

    assertSame(order, result);
    assertEquals(OrderStatus.DELIVERED, result.getStatus());
    verify(customerOrderRepository).save(order);
  }

  @Test
  void statusCancelled_processing_cancelsReservations_andCreatesMovements() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    order.addItem(OrderItem.createFor(product, 2, product.getPrice()));
    order.markProcessing();
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(inventoryService.getInventoryByProductId(2L))
        .thenReturn(Inventory.createFor(product, 10, 1, 2));

    var result = orderService.statusCancelled(1L);

    assertSame(order, result);
    assertEquals(OrderStatus.CANCELLED, result.getStatus());
    verify(inventoryService).cancelReservedQuantity(2L, 2);
    verify(inventoryService).updateLowQuantityFlag(any(Inventory.class));
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
  }

  @Test
  void statusReturned_ok_addsStock_andCreatesMovements() {
    var order = CustomerOrder.create();
    setId(order, 1L);
    var product = product(2L, "SKU-2", new BigDecimal("9.99"));
    order.addItem(OrderItem.createFor(product, 2, product.getPrice()));
    setStatus(order, OrderStatus.DELIVERED);
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(inventoryService.getInventoryByProductId(2L))
        .thenReturn(Inventory.createFor(product, 10, 1, 2));

    var result = orderService.statusReturned(1L);

    assertSame(order, result);
    assertEquals(OrderStatus.RETURNED, result.getStatus());
    verify(inventoryService).addStock(2L, 2);
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
  }

  @Test
  void getCustomerOrderById_missing_throws() {
    when(customerOrderRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.getCustomerOrderById(1L));

    assertEquals("Order with id 1 not found", ex.getMessage());
  }

  @Test
  void getCustomerOrders_capsPageSize() {
    var sort = Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(0, 250, sort);
    var order = CustomerOrder.create();
    setId(order, 1L);
    Page<CustomerOrder> page = new PageImpl<>(List.of(order), PageRequest.of(0, 100, sort), 1);
    when(customerOrderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<CustomerOrderResponse> result = orderService.getCustomerOrders(null, pageable);

    assertEquals(1, result.getTotalElements());
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(customerOrderRepository).findAll(any(Specification.class), pageableCaptor.capture());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(sort, pageableCaptor.getValue().getSort());
  }

  @Test
  void createExternalOrder_missingProduct_throws() {
    var request = externalOrderRequest(
        ExternalOrderSource.WEB_SHOP,
        "EXT-1",
        "Addr",
        List.of(externalItem(1L, 2)));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(1L))).thenReturn(List.of());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> orderService.createExternalOrder(request));

    assertTrue(ex.getMessage().contains("Products not found or deleted"));
  }

  @Test
  void createExternalOrder_dataIntegrity_returnsExisting() {
    var request = externalOrderRequest(
        ExternalOrderSource.WEB_SHOP,
        "EXT-1",
        "Addr",
        List.of(externalItem(1L, 2)));
    var product = product(1L, "SKU-1", new BigDecimal("9.99"));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(1L))).thenReturn(List.of(product));
    when(customerOrderRepository.saveAndFlush(any(CustomerOrder.class)))
        .thenThrow(new DataIntegrityViolationException("dupe"));
    var existing = CustomerOrder.create();
    setId(existing, 99L);
    when(customerOrderRepository.findBySourceAndExternalOrderId(ExternalOrderSource.WEB_SHOP, "EXT-1"))
        .thenReturn(Optional.of(existing));

    var result = orderService.createExternalOrder(request);

    assertSame(existing, result);
    verifyNoInteractions(inventoryService, stockMovementService);
  }

  @Test
  void createExternalOrder_ok_mergesQuantities_andProcesses() {
    var request = externalOrderRequest(
        ExternalOrderSource.WEB_SHOP,
        "EXT-1",
        "Addr",
        List.of(
            externalItem(1L, 2),
            externalItem(1L, 3)));
    var product = product(1L, "SKU-1", new BigDecimal("9.99"));
    var savedOrderRef = new AtomicReference<CustomerOrder>();
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(1L))).thenReturn(List.of(product));
    when(customerOrderRepository.saveAndFlush(any(CustomerOrder.class)))
        .thenAnswer(invocation -> {
          var order = invocation.getArgument(0, CustomerOrder.class);
          setId(order, 10L);
          savedOrderRef.set(order);
          return order;
        });
    when(customerOrderRepository.findById(10L)).thenAnswer(invocation -> Optional.of(savedOrderRef.get()));
    when(inventoryService.reserveStock(1L, 5))
        .thenReturn(Inventory.createFor(product, 10, 1, 2));

    var result = orderService.createExternalOrder(request);

    assertEquals(OrderStatus.PROCESSING, result.getStatus());
    verify(inventoryService).reserveStock(1L, 5);
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
  }

  private static ExternalOrderItemRequest externalItem(Long productId, int quantity) {
    var item = new ExternalOrderItemRequest();
    setField(item, "productId", productId);
    setField(item, "quantity", quantity);
    return item;
  }

  private static ExternalOrderIngestRequest externalOrderRequest(
      ExternalOrderSource source,
      String externalOrderId,
      String shippingAddress,
      List<ExternalOrderItemRequest> items) {
    var request = new ExternalOrderIngestRequest();
    setField(request, "source", source);
    setField(request, "externalOrderId", externalOrderId);
    setField(request, "shippingAddress", shippingAddress);
    setField(request, "items", items);
    return request;
  }

  private static Product product(Long id, String sku, BigDecimal price) {
    var product = Product.create(sku, "Product " + sku, "desc", price);
    setId(product, id);
    return product;
  }

  private static void setId(Object target, Long id) {
    setField(target, "id", id);
  }

  private static void setStatus(CustomerOrder order, OrderStatus status) {
    setField(order, "status", status);
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to set " + fieldName, e);
    }
  }
}
