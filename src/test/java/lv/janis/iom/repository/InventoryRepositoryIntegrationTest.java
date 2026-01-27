package lv.janis.iom.repository;

import lv.janis.iom.config.JpaConfig;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.entity.Product;
import lv.janis.iom.enums.StockStatus;
import lv.janis.iom.repository.specification.InventorySpecifications;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class InventoryRepositoryIntegrationTest {

  @Autowired
  InventoryRepository inventoryRepository;
  @Autowired
  ProductRepository productRepository;

  @Test
  void findAll_appliesStockStatusAndProductNotDeleted() {
    var p1 = productRepository.save(product("SKU-1"));
    var p2 = productRepository.save(product("SKU-2"));
    var p3 = productRepository.save(product("SKU-3"));
    p2.deactivate();
    productRepository.save(p2);

    var inStock = Inventory.createFor(p1, 5, 2, 3);
    var outOfStock = Inventory.createFor(p3, 0, 2, 3);
    var lowStockDeletedProduct = Inventory.createFor(p2, 1, 5, 10);

    inventoryRepository.save(inStock);
    inventoryRepository.save(outOfStock);
    inventoryRepository.save(lowStockDeletedProduct);

    Specification<Inventory> spec = Specification.where(InventorySpecifications.stockStatus(StockStatus.IN_STOCK))
        .and(InventorySpecifications.productNotDeleted());

    var result = inventoryRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(p1.getId(), result.get(0).getProduct().getId());
    assertEquals(5, result.get(0).getQuantity());
  }

  @Test
  void findAll_filtersByAvailableQuantity() {
    var p1 = productRepository.save(product("SKU-3"));
    var inventory = Inventory.createFor(p1, 5, 0, 0);
    inventory.reserveQuantity(2);
    inventoryRepository.save(inventory);

    Specification<Inventory> spec = Specification.where(InventorySpecifications.availableGte(3))
        .and(InventorySpecifications.availableLte(3));

    var result = inventoryRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(3, result.get(0).getAvailableQuantity());
  }

  private static Product product(String sku) {
    return Product.create(sku, "Product " + sku, "desc", new BigDecimal("9.99"));
  }
}
