package lv.janis.iom.service;

import lv.janis.iom.dto.filters.StockMovmentFilter;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.dto.response.StockMovementResponse;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.enums.StockMovementDirection;
import lv.janis.iom.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockMovementServiceTest {
  @Mock
  StockMovementRepository stockMovementRepository;

  @InjectMocks
  StockMovementService stockMovementService;

  @Test
  void createStockMovement_saves() {
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    var request = new StockMovementCreationRequest(
        inventory,
        MovementType.MANUAL_ADJUSTMENT,
        3,
        "cycle count",
        null);
    when(stockMovementRepository.save(any(StockMovement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = stockMovementService.createStockMovement(request);

    assertNotNull(result);
    assertEquals(MovementType.MANUAL_ADJUSTMENT, result.getMovementType());
    var captor = ArgumentCaptor.forClass(StockMovement.class);
    verify(stockMovementRepository).save(captor.capture());
    assertSame(inventory, captor.getValue().getInventory());
    assertEquals(3, captor.getValue().getDelta());
    assertEquals("cycle count", captor.getValue().getReason());
  }

  @Test
  void getStockMovement_nullFilter_mapsResults() {
    Pageable pageable = Pageable.ofSize(20);
    var inventory = Inventory.createFor(product("SKU-1"), 10, 1, 2);
    var movement = new StockMovement(inventory, -2, "shipped", 10L, MovementType.ORDER_FULFILLED);
    setId(movement, 5L);
    Page<StockMovement> page = new PageImpl<>(List.of(movement), pageable, 1);
    when(stockMovementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<StockMovementResponse> result = stockMovementService.getStockMovement(null, pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals(5L, result.getContent().get(0).getId());
    assertEquals(-2, result.getContent().get(0).getDelta());
    verify(stockMovementRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void getStockMovement_withFilter_callsRepository() {
    Pageable pageable = Pageable.ofSize(10);
    Page<StockMovement> page = new PageImpl<>(List.of(), pageable, 0);
    when(stockMovementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    var filter = new StockMovmentFilter();
    filter.setProductId(1L);
    filter.setInventoryId(2L);
    filter.setOrderId(3L);
    filter.setFrom(Instant.now().minusSeconds(60));
    filter.setTo(Instant.now());
    filter.setMovementType(MovementType.ORDER_RESERVED);
    filter.setDirection(StockMovementDirection.OUTBOUND);

    var result = stockMovementService.getStockMovement(filter, pageable);

    assertSame(pageable, result.getPageable());
    verify(stockMovementRepository).findAll(any(Specification.class), eq(pageable));
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }

  private static void setId(StockMovement movement, Long id) {
    try {
      var field = StockMovement.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(movement, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to set id", e);
    }
  }
}
