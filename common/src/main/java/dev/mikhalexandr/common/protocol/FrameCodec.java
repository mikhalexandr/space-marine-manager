package dev.mikhalexandr.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** Кодирует и декодирует length-prefixed фреймы для TCP-обмена. */
public final class FrameCodec {
  private static final int MAX_FRAME_SIZE_BYTES = 8 * 1024 * 1024;

  private FrameCodec() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * @param payload сериализованные данные
   * @return фрейм в формате: 4 байта длины + payload
   */
  public static byte[] toFrame(byte[] payload) {
    if (payload == null) {
      throw new IllegalArgumentException("payload не может быть null");
    }
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + payload.length);
    buffer.putInt(payload.length);
    buffer.put(payload);
    return buffer.array();
  }

  /**
   * Читает один фрейм из потока.
   *
   * @param inputStream входной поток
   * @return байты payload
   * @throws IOException если данные недоступны или формат фрейма некорректен
   */
  public static byte[] readFrame(InputStream inputStream) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(inputStream);
    int payloadSize = dataInputStream.readInt();
    if (payloadSize < 0 || payloadSize > MAX_FRAME_SIZE_BYTES) {
      throw new IOException("Некорректный размер фрейма: " + payloadSize);
    }

    byte[] payload = new byte[payloadSize];
    dataInputStream.readFully(payload);
    return payload;
  }

  /**
   * Записывает один фрейм в поток.
   *
   * @param outputStream выходной поток
   * @param payload сериализованные данные
   */
  public static void writeFrame(OutputStream outputStream, byte[] payload) throws IOException {
    if (payload == null) {
      throw new IllegalArgumentException("payload не может быть null");
    }

    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
    dataOutputStream.writeInt(payload.length);
    dataOutputStream.write(payload);
    dataOutputStream.flush();
  }
}
