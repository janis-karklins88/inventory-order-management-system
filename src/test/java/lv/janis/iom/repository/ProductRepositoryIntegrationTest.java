package lv.janis.iom.repository;

import lv.janis.iom.config.JpaConfig;
import lv.janis.iom.entity.Product;
import lv.janis.iom.repository.specification.ProductSpecifications;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
class ProductRepositoryIntegrationTest {

  @Autowired
  ProductRepository productRepository;

  @Test
  void findAll_appliesSpecs_searchPriceSkuAndNotDeleted() {
    var p1 = Product.create("SKU-1", "Steel Hammer", "desc", new BigDecimal("9.99"));
    var p2 = Product.create("SKU-2", "Plastic Hammer", "desc", new BigDecimal("19.99"));
    p2.deactivate();
    productRepository.save(p1);
    productRepository.save(p2);

    Specification<Product> spec = Specification.where(ProductSpecifications.search("steel"))
        .and(ProductSpecifications.priceLte(new BigDecimal("10.00")))
        .and(ProductSpecifications.skuEquals("SKU-1"))
        .and(ProductSpecifications.notDeleted());

    var result = productRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals("SKU-1", result.get(0).getSku());
  }
}
