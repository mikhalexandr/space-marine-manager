package dev.mikhalexandr.common.security.handshake;

import java.io.Serial;

/**
 * Первое сообщение рукопожатия
 *
 * @param clientId произвольный идентификатор клиента для логов на сервере
 * @param timestampMillis время отправки в миллисекундах
 * @param ephemeralPublicKey X.509-кодированный публичный ECDH-ключ клиента
 */
public record ClientHello(String clientId, long timestampMillis, byte[] ephemeralPublicKey)
    implements HandshakeMessage {
  @Serial private static final long serialVersionUID = 2L;

  @Override
  public ClientHello asClientHello() {
    return this;
  }
}
