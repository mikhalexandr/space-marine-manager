package dev.mikhalexandr.server.network;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/** Состояние одного TCP-клиента между событиями selectorа. */
final class ClientConnection {
  private final ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
  private final String remoteAddress;
  private ByteBuffer payloadBuffer;
  private ByteBuffer responseBuffer;
  private boolean requestInFlight;

  ClientConnection(SocketAddress remoteAddress) {
    this.remoteAddress = String.valueOf(remoteAddress);
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
}
