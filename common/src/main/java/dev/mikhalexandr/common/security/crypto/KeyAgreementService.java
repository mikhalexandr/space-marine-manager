package dev.mikhalexandr.common.security.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

/**
 * Эфемерный обмен ключами через Elliptic Curve Diffie–Hellman
 *
 * <p>Каждая сторона генерирует одноразовую пару EC-ключей, обменивается публичной частью, и
 * локально считает один и тот же общий секрет (без передачи самого секрета по сети)
 *
 * <p>Кривая: {@code secp256r1} (она же NIST P-256)
 */
public final class KeyAgreementService {
  private static final String EC_ALGORITHM = "EC";
  private static final String EC_CURVE = "secp256r1";
  private static final String ECDH_ALGORITHM = "ECDH";
  private static final String TRANSCRIPT_HASH = "SHA-256";
  private static final String AES_ALGORITHM = "AES";
  private static final int AES_KEY_LENGTH_BYTES = 32;
  private static final byte[] INFO_C2S =
      "space-marine-manager c2s".getBytes(StandardCharsets.UTF_8);
  private static final byte[] INFO_S2C =
      "space-marine-manager s2c".getBytes(StandardCharsets.UTF_8);

  private KeyAgreementService() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /** Генерирует одноразовую пару EC-ключей на кривой secp256r1 */
  public static KeyPair generateEphemeralKeyPair() throws IOException {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(EC_ALGORITHM);
      generator.initialize(new ECGenParameterSpec(EC_CURVE));
      return generator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось сгенерировать EC-ключевую пару", e);
    }
  }

  /** Кодирует публичный EC-ключ в стандартный X.509-формат для передачи по сети */
  public static byte[] encodePublicKey(PublicKey publicKey) {
    return publicKey.getEncoded();
  }

  /** Декодирует публичный EC-ключ, полученный по сети, обратно в {@link PublicKey} */
  public static PublicKey decodePublicKey(byte[] encoded) throws IOException {
    try {
      KeyFactory factory = KeyFactory.getInstance(EC_ALGORITHM);
      return factory.generatePublic(new X509EncodedKeySpec(encoded));
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось декодировать EC-публичный ключ", e);
    }
  }

  /**
   * Считает общий секрет ECDH из своего приватного и чужого публичного эфемерных
   * (чётенькое название для одноразовых) ключей
   */
  public static byte[] computeSharedSecret(PrivateKey ourPrivate, PublicKey peerPublic)
      throws IOException {
    try {
      KeyAgreement agreement = KeyAgreement.getInstance(ECDH_ALGORITHM);
      agreement.init(ourPrivate);
      agreement.doPhase(peerPublic, true);
      return agreement.generateSecret();
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось вычислить общий секрет ECDH", e);
    }
  }

  /**
   * Считает transcript-хеш хендшейка: {@code SHA-256(clientEphPub || serverEphPub)}
   */
  public static byte[] transcriptHash(byte[] clientEphPub, byte[] serverEphPub) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance(TRANSCRIPT_HASH);
      digest.update(clientEphPub);
      digest.update(serverEphPub);
      return digest.digest();
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось посчитать transcript-хеш", e);
    }
  }

  /**
   * Выводит пару AES-256 ключей сессии из общего ECDH-секрета через HKDF, привязав их к
   * transcript-хешу
   */
  public static SessionKeys deriveSessionKeys(byte[] sharedSecret, byte[] transcriptHash)
      throws IOException {
    byte[] c2sBytes = Hkdf.derive(transcriptHash, sharedSecret, INFO_C2S, AES_KEY_LENGTH_BYTES);
    byte[] s2cBytes = Hkdf.derive(transcriptHash, sharedSecret, INFO_S2C, AES_KEY_LENGTH_BYTES);
    return new SessionKeys(
        new SecretKeySpec(c2sBytes, AES_ALGORITHM), new SecretKeySpec(s2cBytes, AES_ALGORITHM));
  }
}
