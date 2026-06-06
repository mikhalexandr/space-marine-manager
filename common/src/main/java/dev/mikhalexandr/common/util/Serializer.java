package dev.mikhalexandr.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class Serializer {
  private Serializer() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static byte[] serialize(Object value) throws IOException {
    if (value == null) {
      throw new IllegalArgumentException("value не может быть null");
    }

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
      objectOutputStream.writeObject(value);
      objectOutputStream.flush();
      return outputStream.toByteArray();
    }
  }

  public static <T> T deserialize(byte[] payload, Class<T> expectedType)
      throws IOException, ClassNotFoundException {
    if (payload == null) {
      throw new IllegalArgumentException("payload не может быть null");
    }
    if (expectedType == null) {
      throw new IllegalArgumentException("expectedType не может быть null");
    }

    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(payload);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
      Object value = objectInputStream.readObject();
      if (!expectedType.isInstance(value)) {
        throw new IOException(
            String.format(
                "Ожидался объект типа %s, получен %s",
                expectedType.getName(), value == null ? "null" : value.getClass().getName()));
      }
      return expectedType.cast(value);
    }
  }
}
