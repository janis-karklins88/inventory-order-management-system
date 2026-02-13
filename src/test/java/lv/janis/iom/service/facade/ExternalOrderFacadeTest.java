package lv.janis.iom.service.facade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.dto.requests.ExternalOrderItemRequest;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.ExternalOrderSource;
import lv.janis.iom.enums.OutboxEventStatus;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.OutboxEventRepository;
import lv.janis.iom.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOrderFacadeTest {

  @Mock
  CustomerOrderRepository customerOrderRepository;
  @Mock
  ProductRepository productRepository;
  @Mock
  EntityManager entityManager;
  @Mock
  OutboxEventRepository outboxEventRepository;

  @InjectMocks
  ExternalOrderFacade facade;

  @Test
  void ingest_newOrder_savesOrderAndOutboxEvent() {
    var request = request("EXT-1", List.of(item(1L, 2)));
    var product = product(1L, "SKU-1");

    when(customerOrderRepository.findBySourceAndExternalOrderId(ExternalOrderSource.WEB_SHOP, "EXT-1"))
        .thenReturn(Optional.empty());
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(1L))).thenReturn(List.of(product));
    when(customerOrderRepository.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> {
      var saved = invocation.getArgument(0, CustomerOrder.class);
      setField(saved, "id", 77L);
      return saved;
    });

    Long id = facade.ingest(request);

    assertEquals(77L, id);
    var eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(eventCaptor.capture());
    OutboxEvent event = eventCaptor.getValue();
    assertEquals("EXTERNAL_ORDER_INGESTED", event.getEventType());
    assertEquals(77L, event.getAggregatedId());
    assertEquals(OutboxEventStatus.PENDING, event.getStatus());
    assertEquals("{\"orderId\":77}", event.getPayload());
  }

  @Test
  void ingest_duplicateRequest_returnsExistingAndDoesNotCreateOutbox() {
    var request = request("EXT-1", List.of(item(1L, 2)));
    var existing = CustomerOrder.create();
    setField(existing, "id", 15L);

    when(customerOrderRepository.findBySourceAndExternalOrderId(ExternalOrderSource.WEB_SHOP, "EXT-1"))
        .thenReturn(Optional.of(existing));

    Long id = facade.ingest(request);

    assertEquals(15L, id);
    verify(productRepository, never()).findAllByIdInAndIsDeletedFalse(any());
    verify(customerOrderRepository, never()).saveAndFlush(any(CustomerOrder.class));
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void ingest_dataIntegrityViolation_returnsExistingAndDoesNotCreateOutbox() {
    var request = request("EXT-1", List.of(item(1L, 2)));
    var product = product(1L, "SKU-1");
    var existing = CustomerOrder.create();
    setField(existing, "id", 42L);

    when(customerOrderRepository.findBySourceAndExternalOrderId(ExternalOrderSource.WEB_SHOP, "EXT-1"))
        .thenReturn(Optional.empty(), Optional.of(existing));
    when(productRepository.findAllByIdInAndIsDeletedFalse(Set.of(1L))).thenReturn(List.of(product));
    when(customerOrderRepository.saveAndFlush(any(CustomerOrder.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    Long id = facade.ingest(request);

    assertEquals(42L, id);
    verify(entityManager).clear();
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  @Test
  void ingest_missingProduct_throws() {
    var request = request("EXT-1", List.of(item(1L, 2), item(2L, 1)));
    var onlyOneProduct = product(1L, "SKU-1");

    when(customerOrderRepository.findBySourceAndExternalOrderId(ExternalOrderSource.WEB_SHOP, "EXT-1"))
        .thenReturn(Optional.empty());
    when(productRepository.findAllByIdInAndIsDeletedFalse(eq(Set.of(1L, 2L))))
        .thenReturn(List.of(onlyOneProduct));

    var ex = assertThrows(EntityNotFoundException.class, () -> facade.ingest(request));
    assertTrue(ex.getMessage().contains("Products not found or deleted"));
  }

  private static ExternalOrderIngestRequest request(String externalOrderId, List<ExternalOrderItemRequest> items) {
    var request = new ExternalOrderIngestRequest();
    setField(request, "source", ExternalOrderSource.WEB_SHOP);
    setField(request, "externalOrderId", externalOrderId);
    setField(request, "shippingAddress", "Addr");
    setField(request, "items", items);
    return request;
  }

  private static ExternalOrderItemRequest item(Long productId, int qty) {
    var item = new ExternalOrderItemRequest();
    setField(item, "productId", productId);
    setField(item, "quantity", qty);
    return item;
  }

  private static Product product(Long id, String sku) {
    var product = Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
    setField(product, "id", id);
    return product;
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

