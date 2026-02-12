package lv.janis.iom.dto.response;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lv.janis.iom.entity.CustomerOrder;
import lv.janis.iom.entity.OrderItem;
import lv.janis.iom.enums.FailureCode;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerOrderResponse(
        @Schema(description = "Order id", example = "1001") Long id,
        @Schema(description = "Order status", example = "CREATED") String status,
        @Schema(description = "Total amount for the order", example = "149.99") BigDecimal totalAmount,
        @Schema(description = "Order line items") List<OrderItem> items,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt,
        @Schema(description = "Failure code for rejected/failed orders", example = "OUT_OF_STOCK") FailureCode failureCode,
        @Schema(description = "Failure details for rejected/failed orders", example = "not enough available quantity to reserve") String failureMessage,
        @Schema(description = "Number of processing retries after technical failures", example = "1") Integer retryCount,
        @Schema(description = "Timestamp when order was marked failed or rejected") Instant failedAt
) {
    public static CustomerOrderResponse from(CustomerOrder order) {
        return new CustomerOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getItems(),
                order.getCreatedAt(), 
                order.getUpdatedAt(),
                order.getFailureCode(),
                order.getFailureMessage(),
                order.getRetryCount(),
                order.getFailedAt()
        );
    }
}
