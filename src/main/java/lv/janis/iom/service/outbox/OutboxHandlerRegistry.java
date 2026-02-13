package lv.janis.iom.service.outbox;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import lv.janis.iom.entity.OutboxEvent;
import lv.janis.iom.enums.OutboxEventType;

@Component
public class OutboxHandlerRegistry {
  private final Map<OutboxEventType, Consumer<OutboxEvent>> handlers = new EnumMap<>(OutboxEventType.class);

  public OutboxHandlerRegistry(ExternalOrderOutboxHandler externalOrderHandler) {
    handlers.put(OutboxEventType.EXTERNAL_ORDER_INGESTED, externalOrderHandler::handle);
  }

  public void handle(OutboxEvent event) {
    OutboxEventType eventType;
    try {
      eventType = OutboxEventType.valueOf(event.getEventType());
    } catch (RuntimeException ex) {
      throw new IllegalStateException("No handler for eventType=" + event.getEventType(), ex);
    }

    Consumer<OutboxEvent> handler = handlers.get(eventType);
    if (handler == null) {
      throw new IllegalStateException("No handler for eventType=" + event.getEventType());
    }
    handler.accept(event);
  }
}
