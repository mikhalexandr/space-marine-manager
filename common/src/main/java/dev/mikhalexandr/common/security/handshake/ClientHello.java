package dev.mikhalexandr.common.security.handshake;

import java.io.Serial;

public record ClientHello(String clientId, long timestampMillis, byte[] ephemeralPublicKey)
    implements HandshakeMessage {
  @Serial private static final long serialVersionUID = 2L;

  @Override
  public ClientHello asClientHello() {
    return this;
  }
}
