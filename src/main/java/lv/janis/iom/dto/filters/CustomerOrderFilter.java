package lv.janis.iom.dto.filters;

import lv.janis.iom.enums.OrderStatus;

public class CustomerOrderFilter {
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
