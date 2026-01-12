package lv.janis.iom.enums;
public enum OrderStatus {
    CREATED,        // draft, editable, no inventory
    PROCESSING,     // inventory reserved, payment OK, waiting for shipping
    SHIPPED,        // inventory deducted
    DELIVERED,      // completed
    CANCELLED       // terminal
}