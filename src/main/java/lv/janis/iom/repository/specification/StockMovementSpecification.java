package lv.janis.iom.repository.specification;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import lv.janis.iom.entity.StockMovement;
import lv.janis.iom.enums.MovementType;
import lv.janis.iom.enums.StockMovementDirection;

public class StockMovementSpecification {
    private StockMovementSpecification() {
    }

    public static Specification<StockMovement> createdAfter(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), timestamp);
        };
    }

    public static Specification<StockMovement> createdBefore(Instant timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), timestamp);
        };
    }

    public static Specification<StockMovement> createdBetween(Instant start, Instant end) {
        return Specification.where(createdAfter(start)).and(createdBefore(end));
    }

    public static Specification<StockMovement> search(Long productId, Long inventoryId, Long orderId) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (productId != null) {
                var inventory = root.join("inventory");
                var product = inventory.join("product");
                predicate = cb.and(predicate, cb.equal(product.get("id"), productId));
            }
            if (inventoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("inventory").get("id"), inventoryId));
            }
            if (orderId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("orderId"), orderId));
            }
            return predicate;
        };
    }

    public static Specification<StockMovement> orderStatusEquals(MovementType type) {
        return (root, query, cb) -> {
            if (type == null) return cb.conjunction();
            return cb.equal(root.get("movementType"), type);
        };
    }

    public static Specification<StockMovement> stockMovementDirSpecification(StockMovementDirection direction) {
        return (root, query, cb) -> {
            if (direction == null) return cb.conjunction();
            return direction == StockMovementDirection.OUTBOUND
                ? cb.lt(root.get("delta"), 0)
                : cb.gt(root.get("delta"), 0);
        };
    }




}
