package lv.janis.iom.notification;

import lv.janis.iom.entity.Inventory;

public interface NotificationSender {
    void sendLowStockAlert(Inventory inventory);
}
