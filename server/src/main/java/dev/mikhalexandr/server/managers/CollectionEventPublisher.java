package dev.mikhalexandr.server.managers;

import dev.mikhalexandr.common.dto.event.CollectionEvent;

@FunctionalInterface
public interface CollectionEventPublisher {
  void publish(CollectionEvent event);
}
