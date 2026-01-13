package lv.janis.iom.repository.specification;

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
}
