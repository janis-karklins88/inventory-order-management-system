package lv.janis.iom.service;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.entity.Alert;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.AlertType;
import lv.janis.iom.repository.AlertRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {
  @Mock
  AlertRepository alertRepository;

  @InjectMocks
  AlertService alertService;

  @Test
  void createLowStockAlert_saves() {
    var inventory = Inventory.createFor(product("SKU-1"), 5, 5, 10);
    when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = alertService.createLowStockAlert(inventory);

    assertNotNull(result);
    assertEquals(AlertType.LOW_STOCK, result.getAlertType());
    verify(alertRepository).save(any(Alert.class));
  }

  @Test
  void acknowledgeAlert_missing_throws() {
    when(alertRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> alertService.acknowledgeAlert(1L));

    assertEquals("Alert not found with id: 1", ex.getMessage());
  }

  @Test
  void acknowledgeAlert_ok_marksAcknowledged() {
    var alert = Alert.createLowStockAlert(Inventory.createFor(product("SKU-1"), 5, 5, 10));
    when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

    alertService.acknowledgeAlert(1L);

    assertTrue(alert.isAcknowledged());
  }

  @Test
  void getAlerts_unacknowledgedWithType_usesTypeQuery() {
    Pageable pageable = Pageable.ofSize(10);
    Page<Alert> page = new PageImpl<>(List.of());
    when(alertRepository.findByAlertTypeAndAcknowledgedAtIsNullOrderByCreatedAtDesc(AlertType.LOW_STOCK, pageable))
        .thenReturn(page);

    var result = alertService.getAlerts(true, AlertType.LOW_STOCK, pageable);

    assertSame(page, result);
    verify(alertRepository).findByAlertTypeAndAcknowledgedAtIsNullOrderByCreatedAtDesc(AlertType.LOW_STOCK, pageable);
    verify(alertRepository, never()).findByAcknowledgedAtIsNullOrderByCreatedAtDesc(any());
    verify(alertRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void getAlerts_unacknowledgedOnly_usesUnacknowledgedQuery() {
    Pageable pageable = Pageable.ofSize(10);
    Page<Alert> page = new PageImpl<>(List.of());
    when(alertRepository.findByAcknowledgedAtIsNullOrderByCreatedAtDesc(pageable)).thenReturn(page);

    var result = alertService.getAlerts(true, null, pageable);

    assertSame(page, result);
    verify(alertRepository).findByAcknowledgedAtIsNullOrderByCreatedAtDesc(pageable);
    verify(alertRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void getAlerts_all_usesFindAll() {
    Pageable pageable = Pageable.ofSize(10);
    Page<Alert> page = new PageImpl<>(List.of());
    when(alertRepository.findAll(pageable)).thenReturn(page);

    var result = alertService.getAlerts(false, null, pageable);

    assertSame(page, result);
    verify(alertRepository).findAll(pageable);
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }
}
