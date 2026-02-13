package lv.janis.iom.service.outbox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;

@Component
public class OutboxHandlerRegistry {
  private final Map<String, Consumer<OutboxEvent>> handlers = new HashMap<>();

    public OutboxHandlerRegistry(ExternalOrderOutboxHandler externalOrderHandler) {
        handlers.put("EXTERNAL_ORDER_INGESTED", externalOrderHandler::handle);
    }

    public void handle(OutboxEvent event) {
        Consumer<OutboxEvent> handler = handlers.get(event.getEventType());
        if (handler == null) {
            throw new IllegalStateException("No handler for eventType=" + event.getEventType());
        }
        handler.accept(event);
    }
}
