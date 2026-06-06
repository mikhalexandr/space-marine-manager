package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.dto.event.ServerMessage;
import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.security.crypto.SessionCipher;
import dev.mikhalexandr.common.util.Serializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClientConnection {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnection.class);

  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;
  private final SessionCipher cipher;
  private final String remoteAddress;
  private final Object writeLock = new Object();

  ClientConnection(
      Socket socket,
      InputStream input,
      OutputStream output,
      SessionCipher cipher,
      String remoteAddress) {
    this.socket = socket;
    this.input = input;
    this.output = output;
    this.cipher = cipher;
    this.remoteAddress = remoteAddress;
  }

  InputStream input() {
    return input;
  }

  SessionCipher cipher() {
    return cipher;
  }

  String remoteAddress() {
    return remoteAddress;
  }

  boolean isOpen() {
    return !socket.isClosed();
  }

  void send(ServerMessage message) throws IOException {
    byte[] plaintext = Serializer.serialize(message);
    synchronized (writeLock) {
      FrameCodec.writeFrame(output, cipher.encrypt(plaintext));
    }
  }

  void close() {
    try {
      socket.close();
    } catch (IOException e) {
      LOGGER.debug("Не удалось закрыть сокет клиента {}", remoteAddress, e);
    }
  }
}
