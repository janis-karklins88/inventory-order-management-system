package lv.janis.iom.factory;

import java.util.Objects;
import lv.janis.iom.dto.requests.StockMovementCreationRequest;
import lv.janis.iom.entity.Inventory;
import lv.janis.iom.enums.MovementType;

public final class StockMovementRequestFactory {

  private StockMovementRequestFactory() {
  }

  public static StockMovementCreationRequest orderReserved(Inventory inventory, long orderId, int quantity) {
    requireInventory(inventory);
    requireOrderId(orderId);
    requirePositive(quantity, "quantity");
    return new StockMovementCreationRequest(
        inventory,
        MovementType.ORDER_RESERVED,
        -quantity,
        "Order status changed to PROCESSING",
        orderId);
  }

  public static StockMovementCreationRequest orderFulfilled(Inventory inventory, long orderId, int quantity) {
    requireInventory(inventory);
    requireOrderId(orderId);
    requirePositive(quantity, "quantity");
    return new StockMovementCreationRequest(
        inventory,
        MovementType.ORDER_FULFILLED,
        -quantity,
        "Order status changed to SHIPPED",
        orderId);
  }

  public static StockMovementCreationRequest orderReleased(Inventory inventory, long orderId, int quantity) {
    requireInventory(inventory);
    requireOrderId(orderId);
    requirePositive(quantity, "quantity");
    return new StockMovementCreationRequest(
        inventory,
        MovementType.ORDER_RELEASED,
        quantity,
        "Order status changed to CANCELLED",
        orderId);
  }

  public static StockMovementCreationRequest orderReturned(Inventory inventory, long orderId, int quantity) {
    requireInventory(inventory);
    requireOrderId(orderId);
    requirePositive(quantity, "quantity");
    return new StockMovementCreationRequest(
        inventory,
        MovementType.ORDER_RETURNED,
        quantity,
        "Order status changed to RETURNED",
        orderId);
  }

  public static StockMovementCreationRequest manualAdjustment(Inventory inventory, int delta, String reason) {
    requireInventory(inventory);
    requireNonZero(delta, "delta");
    requireText(reason, "reason");
    return new StockMovementCreationRequest(
        inventory,
        MovementType.MANUAL_ADJUSTMENT,
        delta,
        reason.trim(),
        null);
  }

  private static void requireInventory(Inventory inventory) {
    Objects.requireNonNull(inventory, "inventory is required");
  }

  private static void requireOrderId(long orderId) {
    if (orderId <= 0)
      throw new IllegalArgumentException("orderId must be > 0");
  }

  private static void requirePositive(int value, String name) {
    if (value <= 0)
      throw new IllegalArgumentException(name + " must be > 0");
  }

  private static void requireNonZero(int value, String name) {
    if (value == 0)
      throw new IllegalArgumentException(name + " must be non-zero");
  }

  private static void requireText(String value, String name) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " is required");
    }
  }
}
