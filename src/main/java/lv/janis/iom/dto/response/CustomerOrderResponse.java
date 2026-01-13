package lv.janis.iom.dto.response;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;

public record CustomerOrderResponse(
        Long id,
        String status,
        BigDecimal totalAmount,
        List<OrderItem> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerOrderResponse from(CustomerOrder order) {
        return new CustomerOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getItems(),
                order.getCreatedAt(), 
                order.getUpdatedAt()
        );
    }
}