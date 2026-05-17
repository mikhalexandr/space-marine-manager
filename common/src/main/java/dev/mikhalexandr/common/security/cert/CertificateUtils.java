package dev.mikhalexandr.common.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/** Утилитки для загрузки, кодирования и парсинга X.509-сертификатов */
public final class CertificateUtils {

  private static final String X509_TYPE = "X.509";

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
