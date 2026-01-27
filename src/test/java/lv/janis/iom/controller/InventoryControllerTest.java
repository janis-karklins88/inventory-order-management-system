package lv.janis.iom.controller;

import lv.janis.iom.dto.requests.InventoryAdjustRequest;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.service.InventoryService;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {
  @Autowired
  MockMvc mockMvc;

  @MockBean
  InventoryService inventoryService;

  @Test
  void createInventory_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-1"), 10);
    when(inventoryService.createInventory(eq(1L), any())).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"quantity":10,"reorderLevel":2,"clearLowQuantity":5}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(1))
        .andExpect(jsonPath("$.quantity").value(10));
  }

  @Test
  void getInventoryByProductId_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-2"), 5);
    when(inventoryService.getInventoryByProductId(2L)).thenReturn(inventory);

    mockMvc.perform(get("/api/inventory/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(2))
        .andExpect(jsonPath("$.quantity").value(5));
  }

  @Test
  void listInventory_returnsPage() throws Exception {
    var inventory = inventory(product("SKU-3"), 7);
    var page = new PageImpl<>(List.of(inventory), PageRequest.of(0, 20), 1);
    when(inventoryService.getInventory(any(), any(Pageable.class)))
        .thenReturn(page.map(lv.janis.iom.dto.response.InventoryResponse::from));

    mockMvc.perform(get("/api/inventory?size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].product.id").value(3))
        .andExpect(jsonPath("$.content[0].quantity").value(7));
  }

  @Test
  void listInStockInventory_returnsList() throws Exception {
    var inventory = inventory(product("SKU-4"), 3);
    when(inventoryService.listInStockAllInventory()).thenReturn(List.of(inventory));

    mockMvc.perform(get("/api/inventory/in-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].product.id").value(4))
        .andExpect(jsonPath("$[0].quantity").value(3));
  }

  @Test
  void getAvailableStock_returnsValue() throws Exception {
    when(inventoryService.getAvailableStock(5L)).thenReturn(12);

    mockMvc.perform(get("/api/inventory/5/available"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(12));
  }

  @Test
  void addStock_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-6"), 20);
    when(inventoryService.addStock(6L, 5)).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/6/add?quantity=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(6))
        .andExpect(jsonPath("$.quantity").value(20));
  }

  @Test
  void reduceStock_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-7"), 4);
    when(inventoryService.reduceStock(7L, 2)).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/7/reduce?quantity=2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(7))
        .andExpect(jsonPath("$.quantity").value(4));
  }

  @Test
  void reserveStock_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-8"), 8);
    when(inventoryService.reserveStock(8L, 3)).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/8/reserve?quantity=3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(8))
        .andExpect(jsonPath("$.quantity").value(8));
  }

  @Test
  void cancelReservedQuantity_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-9"), 6);
    when(inventoryService.cancelReservedQuantity(9L, 2)).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/9/reserve/cancel?quantity=2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(9))
        .andExpect(jsonPath("$.quantity").value(6));
  }

  @Test
  void fulfillReservedQuantity_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-10"), 5);
    when(inventoryService.fulfillReservedQuantity(10L, 1)).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/10/reserve/fulfill?quantity=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(10))
        .andExpect(jsonPath("$.quantity").value(5));
  }

  @Test
  void adjustInventoryQuantity_returnsOk() throws Exception {
    var inventory = inventory(product("SKU-11"), 14);
    when(inventoryService.adjustInventoryQuantity(11L, 4, "count")).thenReturn(inventory);

    mockMvc.perform(post("/api/inventory/11/adjust")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"delta":4,"reason":"count"}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.product.id").value(11))
        .andExpect(jsonPath("$.quantity").value(14));
  }

  private static Inventory inventory(Product product, int quantity) {
    var inventory = Inventory.createFor(product, quantity, 1, 2);
    setId(product, productIdFromSku(product.getSku()));
    setId(inventory, productIdFromSku(product.getSku()));
    return inventory;
  }

  private static long productIdFromSku(String sku) {
    return Long.parseLong(sku.replace("SKU-", ""));
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }

  private static void setId(Object target, Long id) {
    ReflectionTestUtils.setField(target, "id", id);
  }
}
