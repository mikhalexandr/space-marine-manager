package dev.mikhalexandr.common.security.crypto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Симметричное шифрование тела сообщений в установленной сессии - AES-256-GCM с двумя ключами и
 * sequence numbers для защиты от replay
 *
 * <p>Формат payload по проводу: {@code IV (12 байт) || ciphertext (с GCM-тегом 16 байт в хвосте)}.
 * Sequence number в кадре не передаётся - обе стороны знают его по контексту
 */
public final class SessionCipher {
  private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_LENGTH_BYTES = 12;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final SecretKey outboundKey;
  private final SecretKey inboundKey;
  private long outboundSeq;
  private long inboundSeq;

  public SessionCipher(SecretKey outboundKey, SecretKey inboundKey) {
    this.outboundKey = outboundKey;
    this.inboundKey = inboundKey;
    this.outboundSeq = 0L;
    this.inboundSeq = 0L;
  }

  /**
   * Шифрует plaintext исходящим ключом, привязывая GCM-тег к текущему outbound-счётчику. Счётчик
   * после успешного шифрования инкрементируется
   */
  public byte[] encrypt(byte[] plaintext) throws IOException {
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      SECURE_RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, outboundKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(longToBytes(outboundSeq));
      byte[] ciphertext = cipher.doFinal(plaintext);
      outboundSeq++;
      byte[] combined = new byte[IV_LENGTH_BYTES + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
      System.arraycopy(ciphertext, 0, combined, IV_LENGTH_BYTES, ciphertext.length);
      return combined;
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось зашифровать payload AES-GCM", e);
    }
  }

  /**
   * Расшифровывает payload вида {@code IV || ciphertext} входящим ключом, ожидая, что AAD равен
   * текущему inbound-счётчику. После успешной расшифровки счётчик инкрементируется
   */
  public byte[] decrypt(byte[] payload) throws IOException {
    if (payload == null || payload.length <= IV_LENGTH_BYTES) {
      throw new IOException("Слишком короткий зашифрованный payload");
    }
    try {
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
      byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, inboundKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(longToBytes(inboundSeq));
      byte[] plaintext = cipher.doFinal(ciphertext);
      inboundSeq++;
      return plaintext;
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось расшифровать payload AES-GCM (replay/повреждение????)", e);
    }
  }

  private static byte[] longToBytes(long value) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(value);
    return buffer.array();
  }
}
