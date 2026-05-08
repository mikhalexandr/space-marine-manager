package dev.mikhalexandr.common.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/** Утилитки для загрузки и парсинга сертификатов и приватных ключей из стандартных форматов */
public final class CertificateUtils {

  private static final String X509_TYPE = "X.509";
  private static final String PKCS12_TYPE = "PKCS12";

  private CertificateUtils() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Читает X.509-сертификат из PEM- или DER-файла
   *
   * @param pemPath путь к файлу сертификата
   * @return распаршенный X.509-сертификат
   */
  public static X509Certificate loadX509Certificate(Path pemPath) throws IOException {
    try (InputStream in = Files.newInputStream(pemPath)) {
      CertificateFactory factory = CertificateFactory.getInstance(X509_TYPE);
      return (X509Certificate) factory.generateCertificate(in);
    } catch (CertificateException e) {
      throw new IOException("Не удалось распарсить X.509-сертификат: " + pemPath, e);
    }
  }

  /**
   * Загружает PKCS12-keystore
   *
   * @param p12Path путь к .p12 файлу
   * @param password пароль keystore
   * @return загруженный {@link KeyStore}
   */
  public static KeyStore loadPkcs12(Path p12Path, char[] password) throws IOException {
    try (InputStream in = Files.newInputStream(p12Path)) {
      KeyStore keyStore = KeyStore.getInstance(PKCS12_TYPE);
      keyStore.load(in, password);
      return keyStore;
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось загрузить PKCS12: " + p12Path, e);
    }
  }

  /**
   * Извлекает приватный ключ из keystore по alias
   *
   * @param keyStore загруженный keystore
   * @param alias имя записи
   * @param password пароль ключа
   * @return приватный ключ
   */
  public static PrivateKey extractPrivateKey(KeyStore keyStore, String alias, char[] password)
      throws IOException {
    try {
      Key key = keyStore.getKey(alias, password);
      if (!(key instanceof PrivateKey privateKey)) {
        throw new IOException("В keystore по alias=" + alias + " нет приватного ключа");
      }
      return privateKey;
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось извлечь приватный ключ по alias=" + alias, e);
    }
  }

  /**
   * Извлекает X.509-сертификат из keystore по alias
   *
   * @param keyStore загруженный keystore
   * @param alias имя записи
   * @return X.509-сертификат
   */
  public static X509Certificate extractCertificate(KeyStore keyStore, String alias)
      throws IOException {
    try {
      Certificate cert = keyStore.getCertificate(alias);
      if (cert == null) {
        throw new IOException("В keystore нет сертификата по alias=" + alias);
      }
      if (!(cert instanceof X509Certificate x509)) {
        throw new IOException("Сертификат по alias=" + alias + " не является X.509");
      }
      return x509;
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось извлечь сертификат по alias=" + alias, e);
    }
  }

  /**
   * Кодирует сертификат в DER-байты для передачи по сети
   *
   * @param cert сертификат
   * @return DER-кодировка сертификата
   */
  public static byte[] encodeCertificate(X509Certificate cert) throws IOException {
    try {
      return cert.getEncoded();
    } catch (CertificateEncodingException e) {
      throw new IOException("Не удалось закодировать сертификат в DER", e);
    }
  }

  /**
   * Декодирует X.509-сертификат из DER-байтов
   *
   * @param der DER-кодировка
   * @return распаршенный X.509-сертификат
   */
  public static X509Certificate decodeCertificate(byte[] der) throws IOException {
    if (der == null || der.length == 0) {
      throw new IOException("Пустые байты сертификата");
    }
    try (InputStream in = new ByteArrayInputStream(der)) {
      CertificateFactory factory = CertificateFactory.getInstance(X509_TYPE);
      return (X509Certificate) factory.generateCertificate(in);
    } catch (CertificateException e) {
      throw new IOException("Не удалось распарсить X.509-сертификат из DER", e);
    }
  }
}
