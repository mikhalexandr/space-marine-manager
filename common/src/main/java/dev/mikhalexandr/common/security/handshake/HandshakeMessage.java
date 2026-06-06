package dev.mikhalexandr.common.security.handshake;

import java.io.IOException;
import java.io.Serializable;

public sealed interface HandshakeMessage extends Serializable permits ClientHello, ServerHello {
  default ClientHello asClientHello() throws IOException {
    throw new IOException("Ожидался ClientHello, получен " + getClass().getName());
  }

  default ServerHello asServerHello() throws IOException {
    throw new IOException("Ожидался ServerHello, получен " + getClass().getName());
  }
}
