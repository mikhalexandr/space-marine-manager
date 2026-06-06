package dev.mikhalexandr.server.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
  private static final String ALGORITHM = "MD2";
  private static final int BYTE_MASK = 0xFF;
  private static final int HEX_WIDTH = 2;

  private PasswordHasher() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static String hash(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return toHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Алгос MD2 недоступен", e);
    }
  }

  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * HEX_WIDTH);
    for (byte value : bytes) {
      String hex = Integer.toHexString(value & BYTE_MASK);
      if (hex.length() < HEX_WIDTH) {
        builder.append('0');
      }
      builder.append(hex);
    }
    return builder.toString();
  }
}
