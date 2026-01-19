package lv.janis.iom.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.enums.StockStatus;

public final class InventorySpecifications {
    private InventorySpecifications() {
    }

    public static Specification<Inventory> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            var product = root.join("product", JoinType.LEFT);
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(product.get("name")), pattern),
                cb.like(cb.lower(product.get("sku")), pattern)
            );
        };
    }

    public static Specification<Inventory> quantityGte(Integer minQuantity) {
        return (root, query, cb) -> {
            if (minQuantity == null) return cb.conjunction();
            return cb.ge(root.get("quantity"), minQuantity);
        };
    }

    public static Specification<Inventory> quantityLte(Integer maxQuantity) {
        return (root, query, cb) -> {
            if (maxQuantity == null) return cb.conjunction();
            return cb.le(root.get("quantity"), maxQuantity);
        };
    }

    public static Specification<Inventory> availableGte(Integer minAvailable) {
        return (root, query, cb) -> {
            if (minAvailable == null) return cb.conjunction();
            return cb.ge(availableExpression(root, cb), minAvailable);
        };
    }

    public static Specification<Inventory> availableLte(Integer maxAvailable) {
        return (root, query, cb) -> {
            if (maxAvailable == null) return cb.conjunction();
            return cb.le(availableExpression(root, cb), maxAvailable);
        };
    }

    public static Specification<Inventory> stockStatus(StockStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            Expression<Number> available = availableExpression(root, cb);
            switch (status) {
                case IN_STOCK:
                    return cb.gt(available, 0);
                case OUT_OF_STOCK:
                    return cb.le(available, 0);
                case LOW_STOCK:
                    return cb.lt(available, root.get("reorderLevel"));
                default:
                    return cb.conjunction();
            }
        };
    }

    public static Specification<Inventory> productNotDeleted() {
        return (root, query, cb) -> {
            var product = root.join("product", JoinType.LEFT);
            return cb.isFalse(product.get("isDeleted"));
        };
    }

    private static Expression<Number> availableExpression(
        jakarta.persistence.criteria.Root<Inventory> root,
        jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        return cb.diff(root.get("quantity"), root.get("reservedQuantity"));
    }
}
