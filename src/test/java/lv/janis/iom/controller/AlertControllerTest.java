package lv.janis.iom.controller;

import lv.janis.iom.dto.response.AlertResponse;
import lv.janis.iom.entity.Alert;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.AlertType;
import lv.janis.iom.service.AlertService;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
public class AlertControllerTest {
  @Autowired
  MockMvc mockMvc;

  @MockBean
  AlertService alertService;

  @Test
  void listAlerts_returnsPage() throws Exception {
    var alert = Alert.createLowStockAlert(Inventory.createFor(product(1L), 5, 5, 10));
    setId(alert, 11L);
    var page = new PageImpl<>(List.of(alert), PageRequest.of(0, 10), 1);
    when(alertService.getAlerts(eq(true), eq(AlertType.LOW_STOCK), any(Pageable.class)))
        .thenReturn(page);

    mockMvc.perform(get("/api/alerts?unacknowledgedOnly=true&type=LOW_STOCK&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(11))
        .andExpect(jsonPath("$.content[0].alertType").value("LOW_STOCK"));
  }

  @Test
  void acknowledgeAlert_returnsNoContent() throws Exception {
    mockMvc.perform(post("/api/alerts/12/acknowledge"))
        .andExpect(status().isNoContent());

    verify(alertService).acknowledgeAlert(12L);
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
