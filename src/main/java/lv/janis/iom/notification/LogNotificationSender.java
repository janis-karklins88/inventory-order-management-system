package lv.janis.iom.notification;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import lv.janis.iom.entity.Inventory;

@Component
public class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void sendLowStockAlert(Inventory inventory) {
        log.warn("LOW STOCK: inventoryId={}, availableQuantity={}, productId={}",
            inventory.getId(),
            inventory.getAvailableQuantity(),
            inventory.getProduct().getId());
    }
}
