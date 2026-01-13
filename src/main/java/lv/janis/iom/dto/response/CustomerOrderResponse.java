package lv.janis.iom.dto.response;
import java.math.BigDecimal;
import java.util.List;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;

public record CustomerOrderResponse(
        Long id,
        String status,
        BigDecimal totalAmount,
        List<OrderItem> items
) {
    public static CustomerOrderResponse from(CustomerOrder order) {
        return new CustomerOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getItems()
        );
    }
}