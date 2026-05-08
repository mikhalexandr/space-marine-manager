package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.security.crypto.SessionCipher;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/** Состояние одного TCP-клиента между событиями selectorа. */
final class ClientConnection {
  private final ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
  private final String remoteAddress;
  private ByteBuffer payloadBuffer;
  private ByteBuffer responseBuffer;
  private boolean requestInFlight;
  private HandshakeStage handshakeStage = HandshakeStage.AWAITING_HELLO;
  private SessionCipher sessionCipher;

  ClientConnection(SocketAddress remoteAddress) {
    this.remoteAddress = String.valueOf(remoteAddress);
  }

  HandshakeStage handshakeStage() {
    return handshakeStage;
  }

  void markEstablished(SessionCipher cipher) {
    this.sessionCipher = cipher;
    handshakeStage = HandshakeStage.ESTABLISHED;
  }

  SessionCipher sessionCipher() {
    return sessionCipher;
  }

  ByteBuffer lengthBuffer() {
    return lengthBuffer;
  }

  ByteBuffer payloadBuffer() {
    return payloadBuffer;
  }

  String remoteAddress() {
    return remoteAddress;
  }

  void preparePayloadBuffer(int payloadLength) {
    payloadBuffer = ByteBuffer.allocate(payloadLength);
  }

  byte[] takePayload() {
    byte[] payload = payloadBuffer.array();
    payloadBuffer = null;
    lengthBuffer.clear();
    return payload;
  }

  void markRequestInFlight() {
    requestInFlight = true;
  }

  boolean requestInFlight() {
    return requestInFlight;
  }

  void completeRequest() {
    requestInFlight = false;
  }

  void enqueueResponse(ByteBuffer responseFrame) {
    responseBuffer = responseFrame;
  }

  ByteBuffer responseBuffer() {
    return responseBuffer;
  }

  void clearResponseBuffer() {
    responseBuffer = null;
  }

  /** Стадия рукопожатия одного соединения. */
  enum HandshakeStage {
    AWAITING_HELLO,
    ESTABLISHED
  }
}
