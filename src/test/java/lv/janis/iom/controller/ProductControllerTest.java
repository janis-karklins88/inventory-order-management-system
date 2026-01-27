package lv.janis.iom.controller;

import lv.janis.iom.entity.Product;
import lv.janis.iom.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ProductService productService;

  @Test
  void createProduct_returnsCreated() throws Exception {
    var product = product("SKU-1", "Name1", new BigDecimal("9.99"));
    setId(product, 1L);
    when(productService.createProduct(any())).thenReturn(product);

    mockMvc.perform(post("/api/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"sku":"SKU-1","name":"Name1","description":"desc","price":9.99}
            """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/products/1")))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sku").value("SKU-1"))
        .andExpect(jsonPath("$.name").value("Name1"))
        .andExpect(jsonPath("$.description").value("desc"))
        .andExpect(jsonPath("$.price").value(9.99));
  }

  @Test
  void listProducts_returnsPage() throws Exception {
    var product = product("SKU-1", "Name1", new BigDecimal("9.99"));
    setId(product, 1L);
    var page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
    when(productService.listProducts(any(), any(Pageable.class)))
        .thenReturn(page.map(p -> lv.janis.iom.dto.response.ProductResponse.from(p)));

    mockMvc.perform(get("/api/products?size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].sku").value("SKU-1"));
  }

  @Test
  void listDeletedProducts_returnsPage() throws Exception {
    var product = product("SKU-2", "Name2", new BigDecimal("19.99"));
    setId(product, 2L);
    var page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
    when(productService.listDeletedProducts(any(Pageable.class)))
        .thenReturn(page.map(p -> lv.janis.iom.dto.response.ProductResponse.from(p)));

    mockMvc.perform(get("/api/products/deleted"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(2))
        .andExpect(jsonPath("$.content[0].sku").value("SKU-2"));
  }

  @Test
  void getProductById_returnsProduct() throws Exception {
    var product = product("SKU-3", "Name3", new BigDecimal("29.99"));
    setId(product, 3L);
    when(productService.getProductById(3L)).thenReturn(product);

    mockMvc.perform(get("/api/products/3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3))
        .andExpect(jsonPath("$.sku").value("SKU-3"));
  }

  @Test
  void updateProduct_returnsProduct() throws Exception {
    var product = product("SKU-4", "Name4", new BigDecimal("39.99"));
    setId(product, 4L);
    when(productService.updateProduct(eq(4L), any())).thenReturn(product);

    mockMvc.perform(put("/api/products/4")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"name":"Name4","description":"desc","price":39.99}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(4))
        .andExpect(jsonPath("$.sku").value("SKU-4"));
  }

  @Test
  void deactivateProduct_returnsProduct() throws Exception {
    var product = product("SKU-5", "Name5", new BigDecimal("49.99"));
    setId(product, 5L);
    product.deactivate();
    when(productService.deactivateProduct(5L)).thenReturn(product);

    mockMvc.perform(post("/api/products/5/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.sku").value("SKU-5"));
  }

  @Test
  void activateProduct_returnsProduct() throws Exception {
    var product = product("SKU-6", "Name6", new BigDecimal("59.99"));
    setId(product, 6L);
    when(productService.activateProduct(6L)).thenReturn(product);

    mockMvc.perform(post("/api/products/6/activate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(6))
        .andExpect(jsonPath("$.sku").value("SKU-6"));
  }

  private static Product product(String sku, String name, BigDecimal price) {
    return Product.create(sku, name, "desc", price);
  }

  private static void setId(Product product, Long id) {
    ReflectionTestUtils.setField(product, "id", id);
  }
}
