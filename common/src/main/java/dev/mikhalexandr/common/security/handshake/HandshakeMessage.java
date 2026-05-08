package dev.mikhalexandr.common.security.handshake;

import java.io.IOException;
import java.io.Serializable;

/** Базовый тип для сообщений хендшейка */
public sealed interface HandshakeMessage extends Serializable permits ClientHello, ServerHello {
  default ClientHello asClientHello() throws IOException {
    throw new IOException("Ожидался ClientHello, получен " + getClass().getName());
  }

  default ServerHello asServerHello() throws IOException {
    throw new IOException("Ожидался ServerHello, получен " + getClass().getName());
  }
}
