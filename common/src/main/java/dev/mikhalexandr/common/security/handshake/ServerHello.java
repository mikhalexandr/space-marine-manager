package dev.mikhalexandr.common.security.handshake;

import java.io.Serial;

/**
 * Ответ сервера на {@link ClientHello}: сертификат сервера, его эфемерный публичный EC-ключ и
 * подпись над {@code clientEphemeralPublicKey || serverEphemeralPublicKey} приватным ключом из
 * сертификата
 *
 * @param certificateDer DER-кодировка X.509 сертификата сервера
 * @param ephemeralPublicKey X.509-кодированный публичный ECDH-ключ сервера
 * @param signature RSA-SHA256 подпись над сцепкой эфемерных ключей клиента и сервера
 */
public record ServerHello(byte[] certificateDer, byte[] ephemeralPublicKey, byte[] signature)
    implements HandshakeMessage {
  @Serial private static final long serialVersionUID = 2L;

  @Override
  public ServerHello asServerHello() {
    return this;
  }
}
