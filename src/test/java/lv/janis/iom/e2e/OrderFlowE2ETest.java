package lv.janis.iom.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false",
    "spring.jackson.serialization.fail-on-empty-beans=false"
})
@AutoConfigureMockMvc
class OrderFlowE2ETest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  ObjectMapper objectMapper;

  @Test
  void product_inventory_order_happyPath() throws Exception {
    long productId = createProduct();
    createInventory(productId);
    long orderId = createOrder();
    addItem(orderId, productId, 2);
    updateStatus(orderId, "processing");
    updateStatus(orderId, "shipped");
    JsonNode delivered = updateStatus(orderId, "delivered");
    assertEquals("DELIVERED", delivered.get("status").asText());
  }

  private long createProduct() throws Exception {
    var response = mockMvc.perform(post("/api/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"sku":"SKU-E2E-1","name":"E2E Product","description":"desc","price":9.99}
            """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private void createInventory(long productId) throws Exception {
    mockMvc.perform(post("/api/inventory/" + productId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"quantity":10,"reorderLevel":2,"clearLowQuantity":5}
            """))
        .andExpect(status().isOk());
  }

  private long createOrder() throws Exception {
    var response = mockMvc.perform(post("/api/orders"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("id").asLong();
  }

  private void addItem(long orderId, long productId, int quantity) throws Exception {
    mockMvc.perform(post("/api/orders/" + orderId + "/items")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"productId":%d,"quantity":%d}
            """.formatted(productId, quantity)))
        .andExpect(status().isOk());
  }

  private JsonNode updateStatus(long orderId, String status) throws Exception {
    var response = mockMvc.perform(post("/api/orders/" + orderId + "/" + status))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response);
  }
}
