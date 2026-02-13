package lv.janis.iom.service.facade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lv.janis.iom.dto.requests.ExternalOrderIngestRequest;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.OutboxEventType;
import lv.janis.iom.repository.CustomerOrderRepository;
import lv.janis.iom.repository.OutboxEventRepository;
import lv.janis.iom.repository.ProductRepository;

@Service
public class ExternalOrderFacade {

  private final CustomerOrderRepository customerOrderRepository;
  private final ProductRepository productRepository;
  private final EntityManager entityManager;
  private final OutboxEventRepository outboxRepo;

  public ExternalOrderFacade(
      CustomerOrderRepository customerOrderRepository,
      ProductRepository productRepository,
      EntityManager entityManager,
      OutboxEventRepository outboxRepo) {

    this.customerOrderRepository = customerOrderRepository;
    this.productRepository = productRepository;
    this.entityManager = entityManager;
    this.outboxRepo = outboxRepo;
  }

  @Transactional
  public Long ingest(ExternalOrderIngestRequest request) {

    var existing = customerOrderRepository
        .findBySourceAndExternalOrderId(
            request.getSource(),
            request.getExternalOrderId());

    if (existing.isPresent()) {
      return existing.get().getId();
    }

    var order = buildOrder(request);

    try {
      customerOrderRepository.saveAndFlush(order);
    } catch (DataIntegrityViolationException ex) {
      entityManager.clear();
      return customerOrderRepository
          .findBySourceAndExternalOrderId(
              request.getSource(),
              request.getExternalOrderId())
          .map(CustomerOrder::getId)
          .orElseThrow(() -> new IllegalStateException(
              "Order from source " + request.getSource() + " with external ID "
                  + request.getExternalOrderId() + " already exists",
              ex));
    }

    Long orderId = Objects.requireNonNull(order.getId(), "Order ID must be present after save");
    outboxRepo.save(OutboxEvent.pending(
        OutboxEventType.EXTERNAL_ORDER_INGESTED,
        order.getId(),
        "{\"orderId\":" + order.getId() + "}"));

    return orderId;

  }

  private CustomerOrder buildOrder(ExternalOrderIngestRequest request) {
    var order = CustomerOrder.create();
    order.setSource(request.getSource());
    order.setExternalOrderId(request.getExternalOrderId());
    order.setShippingAddress(request.getShippingAddress());

    Map<Long, Integer> quantities = sumQuantities(request);
    Map<Long, Product> products = loadProductsOrThrow(quantities.keySet());

    for (var entry : quantities.entrySet()) {
      var product = products.get(entry.getKey());
      var item = OrderItem.createFor(product, entry.getValue(), product.getPrice());
      order.addItem(item);
    }

    return order;
  }

  private Map<Long, Integer> sumQuantities(ExternalOrderIngestRequest request) {
    Map<Long, Integer> quantitiesByProductId = new HashMap<>();
    for (var itemReq : request.getItems()) {
      quantitiesByProductId.merge(itemReq.getProductId(), itemReq.getQuantity(), Integer::sum);
    }
    return quantitiesByProductId;
  }

  private Map<Long, Product> loadProductsOrThrow(Set<Long> productIds) {
    Map<Long, Product> productsById = new HashMap<>();
    for (var product : productRepository.findAllByIdInAndIsDeletedFalse(productIds)) {
      productsById.put(product.getId(), product);
    }
    if (productsById.size() != productIds.size()) {
      var missingIds = new HashSet<>(productIds);
      missingIds.removeAll(productsById.keySet());
      throw new EntityNotFoundException("Products not found or deleted: " + missingIds);
    }
    return productsById;
  }
}
