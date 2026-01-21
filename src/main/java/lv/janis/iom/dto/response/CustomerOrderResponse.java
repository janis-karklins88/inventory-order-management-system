package lv.janis.iom.dto.response;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerOrderResponse(
        @Schema(description = "Order id", example = "1001") Long id,
        @Schema(description = "Order status", example = "CREATED") String status,
        @Schema(description = "Total amount for the order", example = "149.99") BigDecimal totalAmount,
        @Schema(description = "Order line items") List<OrderItem> items,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
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
