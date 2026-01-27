package lv.janis.iom.repository;

import lv.janis.iom.config.JpaConfig;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.enums.StockMovementDirection;
import lv.janis.iom.repository.specification.StockMovementSpecification;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class StockMovementRepositoryIntegrationTest {

  @Autowired
  StockMovementRepository stockMovementRepository;
  @Autowired
  InventoryRepository inventoryRepository;
  @Autowired
  ProductRepository productRepository;
  @Autowired
  EntityManager entityManager;

  @Test
  void findAll_filtersByDirectionAndType() {
    var product = productRepository.save(product("SKU-1"));
    var inventory = inventoryRepository.save(Inventory.createFor(product, 10, 1, 2));

    var inbound = new StockMovement(inventory, 5, "restock", null, MovementType.MANUAL_ADJUSTMENT);
    var outbound = new StockMovement(inventory, -2, "reserved", 100L, MovementType.ORDER_RESERVED);
    stockMovementRepository.save(inbound);
    stockMovementRepository.save(outbound);

    Specification<StockMovement> spec = Specification.where(
        StockMovementSpecification.stockMovementDirSpecification(StockMovementDirection.OUTBOUND))
        .and(StockMovementSpecification.orderStatusEquals(MovementType.ORDER_RESERVED));

    var result = stockMovementRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(MovementType.ORDER_RESERVED, result.get(0).getMovementType());
    assertEquals(-2, result.get(0).getDelta());
  }

  @Test
  void findAll_filtersByCreatedBetween() {
    var product = productRepository.save(product("SKU-2"));
    var inventory = inventoryRepository.save(Inventory.createFor(product, 10, 1, 2));

    var earlier = new StockMovement(inventory, 1, "adjust", null, MovementType.MANUAL_ADJUSTMENT);
    var later = new StockMovement(inventory, 1, "adjust", null, MovementType.MANUAL_ADJUSTMENT);
    stockMovementRepository.save(earlier);
    stockMovementRepository.save(later);
    setCreatedAt(earlier, Instant.now().minusSeconds(7200));
    setCreatedAt(later, Instant.now().minusSeconds(300));
    entityManager.flush();
    entityManager.clear();

    var now = Instant.now();
    Specification<StockMovement> spec = Specification.where(
        StockMovementSpecification.createdBetween(now.minusSeconds(1800), now));

    var result = stockMovementRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(later.getId(), result.get(0).getId());
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }

  private void setCreatedAt(StockMovement movement, Instant instant) {
    entityManager.createQuery(
            "update StockMovement sm set sm.createdAt = :createdAt where sm.id = :id")
        .setParameter("createdAt", instant)
        .setParameter("id", movement.getId())
        .executeUpdate();
  }
}
