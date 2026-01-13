package lv.janis.iom.repository.specification;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.enums.OrderStatus;

public class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<CustomerOrder> orderStatusEquals(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<CustomerOrder> createdAfter(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), timestamp);
        };
    }

    public static Specification<CustomerOrder> createdBefore(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), timestamp);
        };
    }

    public static Specification<CustomerOrder> createdBetween(Instant start, Instant end) {
        return Specification.where(createdAfter(start)).and(createdBefore(end));
    }

    public static Specification<CustomerOrder> updatedAfter(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("updatedAt"), timestamp);
        };
    }

    public static Specification<CustomerOrder> updatedBefore(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("updatedAt"), timestamp);
        };
    }

    public static Specification<CustomerOrder> updatedBetween(Instant start, Instant end) {
        return Specification.where(updatedAfter(start)).and(updatedBefore(end));
    }
}
