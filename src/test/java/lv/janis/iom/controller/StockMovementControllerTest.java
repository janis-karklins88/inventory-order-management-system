package lv.janis.iom.controller;

import lv.janis.iom.dto.response.StockMovementResponse;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.service.StockMovementService;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockMovementController.class)
public class StockMovementControllerTest {
  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  StockMovementService stockMovementService;

  @Test
  void listStockMovements_returnsPage() throws Exception {
    var product = product(1L);
    var inventory = Inventory.createFor(product, 10, 1, 2);
    setId(inventory, 2L);
    var movement = new StockMovement(inventory, -3, "shipped", 99L, MovementType.ORDER_FULFILLED);
    setId(movement, 3L);
    var page = new PageImpl<>(List.of(StockMovementResponse.from(movement)), PageRequest.of(0, 10), 1);
    when(stockMovementService.getStockMovement(any(), any(Pageable.class))).thenReturn(page);

    mockMvc.perform(get("/api/stock-movements?size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(3))
        .andExpect(jsonPath("$.content[0].inventoryId").value(2))
        .andExpect(jsonPath("$.content[0].productId").value(1))
        .andExpect(jsonPath("$.content[0].orderId").value(99))
        .andExpect(jsonPath("$.content[0].movementType").value("ORDER_FULFILLED"));
  }

  private static Product product(Long id) {
    var product = Product.create("SKU-" + id, "Product " + id, "desc", new BigDecimal("9.99"));
    setId(product, id);
    return product;
  }

  private static void setId(Object target, Long id) {
    ReflectionTestUtils.setField(target, "id", id);
  }
}
