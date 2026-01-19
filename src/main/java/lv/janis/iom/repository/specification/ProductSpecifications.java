package lv.janis.iom.repository.specification;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import lv.janis.iom.entity.Product;

public final class ProductSpecifications {
    private ProductSpecifications() {
    }
    
    public static Specification<Product> search(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("sku")), pattern)
            );
        };
    }

    public static Specification<Product> priceGte(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> priceLte(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> skuEquals(String sku) {
        return (root, query, cb) -> {
            if (sku == null || sku.isBlank()) return cb.conjunction();
            return cb.equal(root.get("sku"), sku.trim());
        };
    }

    public static Specification<Product> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Product> deletedOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("isDeleted"));
    }
}
