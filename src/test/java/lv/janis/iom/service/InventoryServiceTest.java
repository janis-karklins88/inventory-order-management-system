package lv.janis.iom.service;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.filters.InventoryFilter;
import lv.janis.iom.dto.requests.InventoryCreationRequest;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.dto.response.InventoryResponse;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.repository.AlertRepository;
import lv.janis.iom.repository.InventoryRepository;
import lv.janis.iom.repository.NotificationTaskRepository;
import lv.janis.iom.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
  @Mock
  InventoryRepository inventoryRepository;
  @Mock
  ProductRepository productRepository;
  @Mock
  StockMovementService stockMovementService;
  @Mock
  NotificationTaskRepository notificationTaskRepository;
  @Mock
  AlertRepository alertRepository;

  @InjectMocks
  InventoryService inventoryService;

  @SuppressWarnings("null")
  @Test
  void createInventory_nullProductId_throws() {
    var req = new InventoryCreationRequest();
    req.setQuantity(1);
    req.setReorderLevel(1);

    InventoryService raw = inventoryService;
    var ex = assertThrows(IllegalArgumentException.class, () -> raw.createInventory(null, req));

    assertEquals("productId is required", ex.getMessage());
    verifyNoInteractions(inventoryRepository, productRepository);
  }

  @Test
  void createInventory_nullRequest_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.createInventory(1L, null));
    assertEquals("request is required", ex.getMessage());
    verifyNoInteractions(inventoryRepository, productRepository);
  }

  @Test
  void createInventory_negativeQuantity_throws() {
    var req = new InventoryCreationRequest();
    req.setQuantity(-1);
    req.setReorderLevel(1);

    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.createInventory(1L, req));
    assertEquals("quantity must be zero or positive", ex.getMessage());
    verifyNoInteractions(inventoryRepository, productRepository);
  }

  @Test
  void createInventory_existingInventory_returnsExisting() {
    var req = new InventoryCreationRequest();
    req.setQuantity(1);
    req.setReorderLevel(1);
    req.setClearLowQuantity(0);

    var existing = Inventory.createFor(product("SKU-1"), 5, 2, 4);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(existing));

    var result = inventoryService.createInventory(1L, req);

    assertSame(existing, result);
    verify(inventoryRepository).findByProductId(1L);
    verifyNoInteractions(productRepository);
    verify(inventoryRepository, never()).saveAndFlush(any());
  }

  @Test
  void createInventory_creates_saves_andSetsLowFlag() {
    var req = new InventoryCreationRequest();
    req.setQuantity(5);
    req.setReorderLevel(5);

    var product = product("SKU-1");
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(inventoryRepository.saveAndFlush(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.createInventory(1L, req);

    assertSame(product, result.getProduct());
    assertTrue(result.isLowQuantity());
    assertEquals(0, result.getClearLowQuantity());
    verify(productRepository).findById(1L);
    verify(inventoryRepository).saveAndFlush(result);
  }

  @Test
  void createInventory_dataIntegrityViolation_returnsExisting() {
    var req = new InventoryCreationRequest();
    req.setQuantity(5);
    req.setReorderLevel(1);
    req.setClearLowQuantity(0);

    var product = product("SKU-1");
    var existing = Inventory.createFor(product, 10, 2, 5);
    when(inventoryRepository.findByProductId(1L))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(inventoryRepository.saveAndFlush(any(Inventory.class)))
        .thenThrow(new DataIntegrityViolationException("dupe"));

    var result = inventoryService.createInventory(1L, req);

    assertSame(existing, result);
    verify(inventoryRepository).saveAndFlush(any(Inventory.class));
  }

  @Test
  void getInventoryByProductId_missing_throws() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> inventoryService.getInventoryByProductId(1L));

    assertEquals("Inventory for product id 1 not found", ex.getMessage());
  }

  @Test
  void getInventoryByProductId_ok_returns() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    var result = inventoryService.getInventoryByProductId(1L);

    assertSame(inventory, result);
  }

  @Test
  void addStock_nullQuantity_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.addStock(1L, null));
    assertEquals("quantityToAdd is required", ex.getMessage());
    verifyNoInteractions(inventoryRepository);
  }

  @Test
  void addStock_updates_andSaves() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 5, 7);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.addStock(1L, 3);

    assertSame(inventory, result);
    assertEquals(13, inventory.getQuantity());
    verify(inventoryRepository).save(inventory);
  }

  @Test
  void reduceStock_triggersLowStockNotification() {
    var inventory = Inventory.createFor(product("SKU-1"), 6, 5, 8);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.reduceStock(1L, 1);

    assertSame(inventory, result);
    assertTrue(inventory.isLowQuantity());
    verify(notificationTaskRepository).save(any());
    verify(alertRepository).save(any());
  }

  @Test
  void cancelReservedQuantity_updates_andSaves() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 3, 4);
    inventory.reserveQuantity(4);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.cancelReservedQuantity(1L, 2);

    assertSame(inventory, result);
    assertEquals(2, inventory.getReservedQuantity());
    assertEquals(8, inventory.getAvailableQuantity());
    verify(inventoryRepository).save(inventory);
  }

  @Test
  void fulfillReservedQuantity_triggersLowStockNotification() {
    var inventory = Inventory.createFor(product("SKU-1"), 6, 5, 8);
    inventory.reserveQuantity(2);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.fulfillReservedQuantity(1L, 2);

    assertSame(inventory, result);
    assertTrue(inventory.isLowQuantity());
    verify(notificationTaskRepository).save(any());
    verify(alertRepository).save(any());
  }

  @Test
  void getAvailableStock_ok_returnsAvailable() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    inventory.reserveQuantity(3);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    var available = inventoryService.getAvailableStock(1L);

    assertEquals(7, available);
  }

  @Test
  void getInventory_capsPageSize_andMaps() {
    var sort = Sort.by("quantity").descending();
    Pageable pageable = PageRequest.of(0, 200, sort);

    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    Page<Inventory> page = new PageImpl<>(List.of(inventory), PageRequest.of(0, 100, sort), 1);
    when(inventoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<InventoryResponse> result = inventoryService.getInventory(null, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals("SKU-1", result.getContent().get(0).getProduct().getSku());
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(inventoryRepository).findAll(any(Specification.class), pageableCaptor.capture());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(sort, pageableCaptor.getValue().getSort());
  }

  @Test
  void listInStockAllInventory_returns() {
    var inventory = Inventory.createFor(product("SKU-1"), 1, 0, 0);
    when(inventoryRepository.findAllInStockWithProduct()).thenReturn(List.of(inventory));

    var result = inventoryService.listInStockAllInventory();

    assertEquals(1, result.size());
    assertSame(inventory, result.get(0));
    verify(inventoryRepository).findAllInStockWithProduct();
  }

  @Test
  void adjustInventoryQuantity_nullDelta_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.adjustInventoryQuantity(1L, null, "reason"));
    assertEquals("delta is required", ex.getMessage());
  }

  @Test
  void adjustInventoryQuantity_zeroDelta_throws() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.adjustInventoryQuantity(1L, 0, "reason"));
    assertEquals("delta cannot be zero", ex.getMessage());
  }

  @Test
  void adjustInventoryQuantity_blankReason_throws() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.adjustInventoryQuantity(1L, 1, "  "));
    assertEquals("reason is required", ex.getMessage());
  }

  @Test
  void adjustInventoryQuantity_positive_createsMovement_andSaves() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.adjustInventoryQuantity(1L, 3, "cycle count");

    assertSame(inventory, result);
    assertEquals(13, inventory.getQuantity());
    var captor = ArgumentCaptor.forClass(StockMovementCreationRequest.class);
    verify(stockMovementService).createStockMovement(captor.capture());
    assertEquals(MovementType.MANUAL_ADJUSTMENT, captor.getValue().getMovementType());
    assertEquals(3, captor.getValue().getDelta());
    assertEquals("cycle count", captor.getValue().getReason());
  }

  @Test
  void adjustInventoryQuantity_negative_triggersLowStockNotification() {
    var inventory = Inventory.createFor(product("SKU-1"), 6, 5, 8);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = inventoryService.adjustInventoryQuantity(1L, -1, "damage");

    assertSame(inventory, result);
    assertTrue(inventory.isLowQuantity());
    verify(stockMovementService).createStockMovement(any(StockMovementCreationRequest.class));
    verify(notificationTaskRepository).save(any());
    verify(alertRepository).save(any());
  }

  @Test
  void updateLowQuantityFlag_nullInventory_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> inventoryService.updateLowQuantityFlag(null));
    assertEquals("inventory is required", ex.getMessage());
  }

  @Test
  void updateLowQuantityFlag_clearsLowWhenAboveClearQuantity() {
    var inventory = Inventory.createFor(product("SKU-1"), 20, 5, 10);
    inventory.setIsLowQuantity(true);

    inventoryService.updateLowQuantityFlag(inventory);

    assertFalse(inventory.isLowQuantity());
  }

  @Test
  void updateLowQuantityFlag_setsLowWhenBelowReorderLevel() {
    var inventory = Inventory.createFor(product("SKU-1"), 5, 5, 10);

    inventoryService.updateLowQuantityFlag(inventory);

    assertTrue(inventory.isLowQuantity());
  }

  @Test
  void lowStockCheck_whenAlreadyLow_doesNothing() {
    var inventory = Inventory.createFor(product("SKU-1"), 5, 5, 10);
    inventory.setIsLowQuantity(true);

    inventoryService.lowStockCheck(inventory, true);

    verifyNoInteractions(notificationTaskRepository, alertRepository);
  }

  @Test
  void getInventory_withFilter_keepsPageSize() {
    Pageable pageable = PageRequest.of(0, 50, Sort.by("quantity"));
    Page<Inventory> page = new PageImpl<>(List.of(), pageable, 0);
    when(inventoryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    var filter = new InventoryFilter();
    filter.setQ("sku");

    Page<InventoryResponse> result = inventoryService.getInventory(filter, pageable);

    assertSame(pageable, result.getPageable());
    verify(inventoryRepository).findAll(any(Specification.class), eq(pageable));
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }
}
