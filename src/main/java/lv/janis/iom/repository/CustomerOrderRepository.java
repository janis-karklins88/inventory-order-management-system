package lv.janis.iom.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.iom.entity.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
}
