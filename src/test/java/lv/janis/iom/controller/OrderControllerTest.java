package lv.janis.iom.controller;

import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.requests.ExternalOrderItemRequest;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.service.OrderService;
import lv.janis.iom.service.facade.ExternalOrderFacade;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {
  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  OrderService orderService;
  @MockitoBean
  ExternalOrderFacade externalOrderFacade;

  @Test
  void createOrder_returnsCreated() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 1L);
    when(orderService.createOrder()).thenReturn(order);

    mockMvc.perform(post("/api/orders"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/orders/1")))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("CREATED"));
  }

  @Test
  void createExternalOrder_returnsOk() throws Exception {
    when(externalOrderFacade.ingest(any(ExternalOrderIngestRequest.class))).thenReturn(2L);

    mockMvc.perform(post("/api/orders/external")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "source":"WEB_SHOP",
              "externalOrderId":"EXT-1",
              "shippingAddress":"Addr",
              "items":[{"productId":1,"quantity":2}]
            }
            """))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Location", endsWith("/api/orders/2")));
  }

  @Test
  void addItem_returnsOk() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 3L);
    when(orderService.addItem(eq(3L), eq(4L), eq(2))).thenReturn(order);

    mockMvc.perform(post("/api/orders/3/items")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"productId":4,"quantity":2}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3));
  }

  @Test
  void removeItem_returnsOk() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 4L);
    when(orderService.removeItem(4L, 10L)).thenReturn(order);

    mockMvc.perform(delete("/api/orders/4/items/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4));
  }

  @Test
  void statusTransitions_returnOk() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 5L);
    when(orderService.statusProcessing(5L)).thenReturn(order);
    when(orderService.statusShipped(5L)).thenReturn(order);
    when(orderService.statusDelivered(5L)).thenReturn(order);
    when(orderService.statusCancelled(5L)).thenReturn(order);
    when(orderService.statusReturned(5L)).thenReturn(order);

    mockMvc.perform(post("/api/orders/5/processing"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/orders/5/shipped"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/orders/5/delivered"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/orders/5/cancelled"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/orders/5/returned"))
        .andExpect(status().isOk());
  }

  @Test
  void getCustomerOrderById_returnsOk() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 6L);
    when(orderService.getCustomerOrderById(6L)).thenReturn(order);

    mockMvc.perform(get("/api/orders/6"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(6));
  }

  @Test
  void listOrders_returnsPage() throws Exception {
    var order = CustomerOrder.create();
    setId(order, 7L);
    var page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
    when(orderService.getCustomerOrders(any(), any(Pageable.class)))
        .thenReturn(page.map(lv.janis.iom.dto.response.CustomerOrderResponse::from));

    mockMvc.perform(get("/api/orders?size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(7));
  }

  private static void setId(CustomerOrder order, Long id) {
    ReflectionTestUtils.setField(order, "id", id);
  }

  @SuppressWarnings("unused")
  private static ExternalOrderIngestRequest externalOrderRequest() {
    var request = new ExternalOrderIngestRequest();
    setField(request, "source", ExternalOrderSource.WEB_SHOP);
    setField(request, "externalOrderId", "EXT-1");
    setField(request, "shippingAddress", "Addr");
    setField(request, "items", List.of(externalItem(1L, 2)));
    return request;
  }

  private static ExternalOrderItemRequest externalItem(Long productId, int quantity) {
    var item = new ExternalOrderItemRequest();
    setField(item, "productId", productId);
    setField(item, "quantity", quantity);
    return item;
  }

  @SuppressWarnings("unused")
  private static Product product(Long id) {
    var product = Product.create("SKU-" + id, "Product " + id, "desc", new BigDecimal("9.99"));
    ReflectionTestUtils.setField(product, "id", id);
    return product;
  }

  @SuppressWarnings("unused")
  private static OrderItem item(Product product, int qty) {
    return OrderItem.createFor(product, qty, product.getPrice());
  }

  private static void setField(Object target, String fieldName, Object value) {
    ReflectionTestUtils.setField(target, fieldName, value);
  }
}
