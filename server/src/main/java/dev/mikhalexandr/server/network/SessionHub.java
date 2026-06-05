package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.event.CollectionEvent;
import dev.mikhalexandr.common.dto.event.ServerMessage;
import dev.mikhalexandr.server.managers.CollectionEventPublisher;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Так называемый реестр активных клиентских соединений и рассылка серверных событий коллекции
 * (push)
 */
final class SessionHub implements CollectionEventPublisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(SessionHub.class);

  private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();

  void register(ClientConnection connection) {
    connections.add(connection);
    LOGGER.debug(
        "Соединение зарегистрировано в hub: {} (всего {})",
        connection.remoteAddress(),
        connections.size());
  }

  void unregister(ClientConnection connection) {
    connections.remove(connection);
    LOGGER.debug(
        "Соединение удалено из хаба: {} (осталось {})",
        connection.remoteAddress(),
        connections.size());
  }

  @Override
  public void publish(CollectionEvent event) {
    if (event == null || connections.isEmpty()) {
      return;
    }
    ServerMessage message = ServerMessage.event(event);
    for (ClientConnection connection : connections) {
      deliver(connection, message);
    }
  }

  private void deliver(ClientConnection connection, ServerMessage message) {
    try {
      connection.send(message);
    } catch (IOException e) {
      LOGGER.info(
          "Не удалось доставить событие {}: {} - соединение закрывается",
          connection.remoteAddress(),
          e.getMessage());
      connections.remove(connection);
      connection.close();
    }
  }
}
