package lv.janis.iom.repository;

import lv.janis.iom.config.JpaConfig;
import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.OrderStatus;
import lv.janis.iom.repository.specification.OrderSpecifications;

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
class CustomerOrderRepositoryIntegrationTest {

  @Autowired
  CustomerOrderRepository customerOrderRepository;
  @Autowired
  EntityManager entityManager;

  @Test
  void findAll_filtersByStatus() {
    var created = CustomerOrder.create();
    var processing = CustomerOrder.create();
    processing.markProcessing();

    customerOrderRepository.save(created);
    customerOrderRepository.save(processing);

    Specification<CustomerOrder> spec = Specification.where(
        OrderSpecifications.orderStatusEquals(OrderStatus.PROCESSING));

    var result = customerOrderRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(OrderStatus.PROCESSING, result.get(0).getStatus());
  }

  @Test
  void findAll_filtersByCreatedBetween() {
    var older = CustomerOrder.create();
    var newer = CustomerOrder.create();

    var now = Instant.now();
    customerOrderRepository.save(older);
    customerOrderRepository.save(newer);
    setTimestamps(older, now.minusSeconds(7200));
    setTimestamps(newer, now.minusSeconds(600));
    entityManager.flush();
    entityManager.clear();

    Specification<CustomerOrder> spec = Specification.where(
        OrderSpecifications.createdBetween(now.minusSeconds(1800), now));

    var result = customerOrderRepository.findAll(spec);

    assertEquals(1, result.size());
    assertEquals(newer.getId(), result.get(0).getId());
  }

  private void setTimestamps(CustomerOrder order, Instant instant) {
    entityManager.createQuery(
            "update CustomerOrder co set co.createdAt = :createdAt, co.updatedAt = :updatedAt where co.id = :id")
        .setParameter("createdAt", instant)
        .setParameter("updatedAt", instant)
        .setParameter("id", order.getId())
        .executeUpdate();
  }
}
