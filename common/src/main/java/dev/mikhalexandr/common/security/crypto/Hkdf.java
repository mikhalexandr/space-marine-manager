package dev.mikhalexandr.common.security.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HKDF (HMAC-based Extract-and-Expand Key Derivation Function), RFC 5869
 *
 * <p>Двухступенчатый KDF:
 *
 * <ol>
 *   <li>{@code Extract(salt, ikm) -> prk} - собирает входной материал в равномерный псевдослучайный
 *       ключ
 *   <li>{@code Expand(prk, info, length) -> okm} - растягивает PRK в нужное количество байт,
 *       привязанных к контексту {@code info}
 * </ol>
 */
public final class Hkdf {
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int HASH_LENGTH = 32;
  private static final int MAX_OUTPUT_BLOCKS = 255;

  private Hkdf() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Полная процедура HKDF: Extract + Expand за один вызов
   *
   * @param salt соль; может быть {@code null} или пустым массивом - тогда внутри подставится
   *     32-байтный нулевой массив, как требует RFC 5869
   * @param ikm входной ключевой материал (общий секрет ECDH)
   * @param info контекстная метка (c2s, s2c)
   * @param length желаемая длина выхода в байтах (тут для AES-256 это 256 бит = 32 байта)
   * @return {@code length} байт выходного ключевого материала
   */
  public static byte[] derive(byte[] salt, byte[] ikm, byte[] info, int length) throws IOException {
    return expand(extract(salt, ikm), info, length);
  }

  private static byte[] extract(byte[] salt, byte[] ikm) throws IOException {
    byte[] saltOrZero = (salt == null || salt.length == 0) ? new byte[HASH_LENGTH] : salt;
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(saltOrZero, HMAC_ALGORITHM));
      return mac.doFinal(ikm);
    } catch (GeneralSecurityException e) {
      throw new IOException("HKDF-Extract упал", e);
    }
  }

  private static byte[] expand(byte[] prk, byte[] info, int length) throws IOException {
    int blocks = (length + HASH_LENGTH - 1) / HASH_LENGTH;
    if (blocks > MAX_OUTPUT_BLOCKS) {
      throw new IOException("HKDF: запрошено слишком много байт: " + length);
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(prk, HMAC_ALGORITHM));
      ByteArrayOutputStream out = new ByteArrayOutputStream(blocks * HASH_LENGTH);
      byte[] previous = new byte[0];
      byte[] safeInfo = info == null ? new byte[0] : info;
      for (int i = 1; i <= blocks; i++) {
        mac.update(previous);
        mac.update(safeInfo);
        mac.update((byte) i);
        previous = mac.doFinal();
        out.write(previous);
      }
      byte[] full = out.toByteArray();
      byte[] result = new byte[length];
      System.arraycopy(full, 0, result, 0, length);
      return result;
    } catch (GeneralSecurityException e) {
      throw new IOException("HKDF-Expand упал", e);
    }
  }
}
