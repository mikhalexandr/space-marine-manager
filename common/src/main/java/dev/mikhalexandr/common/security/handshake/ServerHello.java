package dev.mikhalexandr.common.security.handshake;

import java.io.Serial;

public record ServerHello(byte[] certificateDer, byte[] ephemeralPublicKey, byte[] signature)
    implements HandshakeMessage {
  @Serial private static final long serialVersionUID = 2L;

  @Override
  public ServerHello asServerHello() {
    return this;
  }
}
