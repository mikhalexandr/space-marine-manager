package dev.mikhalexandr.server.security;

import dev.mikhalexandr.common.security.cert.CertificateUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Криптографическая личность сервера: приватный ключ + сертификат, подписанный CA
 *
 * <p>Загружается один раз при старте из PKCS12-keystore
 */
public final class ServerIdentity {
  private final PrivateKey privateKey;
  private final X509Certificate certificate;

  private ServerIdentity(PrivateKey privateKey, X509Certificate certificate) {
    this.privateKey = privateKey;
    this.certificate = certificate;
  }

  /**
   * Загружает идентичность сервера из PKCS12
   *
   * @param p12Path путь к {@code server.p12}
   * @param password пароль keystore
   * @param alias имя записи в keystore
   * @return загруженная идентичность
   */
  public static ServerIdentity loadFromPkcs12(Path p12Path, char[] password, String alias)
      throws IOException {
    KeyStore keyStore = CertificateUtils.loadPkcs12(p12Path, password);
    PrivateKey key = CertificateUtils.extractPrivateKey(keyStore, alias, password);
    X509Certificate cert = CertificateUtils.extractCertificate(keyStore, alias);
    return new ServerIdentity(key, cert);
  }

  public PrivateKey privateKey() {
    return privateKey;
  }

  public X509Certificate certificate() {
    return certificate;
  }
}
