package lv.janis.iom.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import org.springframework.data.jpa.domain.Specification;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.ExternalOrderSource;

import java.util.Optional;


public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    // Fetch orders with items and their associated products to avoid N+1 problem
    @EntityGraph(attributePaths = {"items", "items.product"})
    @NonNull
    Page<CustomerOrder> findAll(@Nullable Specification<CustomerOrder> spec,@NonNull Pageable pageable);

    Optional<CustomerOrder> findBySourceAndExternalOrderId(ExternalOrderSource source, String externalOrderId);
}

