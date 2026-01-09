package lv.janis.iom.dto.response;

import java.math.BigDecimal;
import lv.janis.iom.entity.Product;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getPrice()
        );
    }
}
