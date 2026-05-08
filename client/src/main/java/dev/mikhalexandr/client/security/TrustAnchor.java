package dev.mikhalexandr.client.security;

import dev.mikhalexandr.common.security.cert.CertificateUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * Так называемый якорь доверия клиента: публичный сертификат CA, через который проверяются все серверные
 * сертификаты
 */
public final class TrustAnchor {
  /** Тип SAN-записи "dNSName" по RFC 5280 */
  private static final int SAN_TYPE_DNS = 2;

  /** Тип SAN-записи "iPAddress" по RFC 5280 */
  private static final int SAN_TYPE_IP = 7;

  private final X509Certificate caCertificate;

  private TrustAnchor(X509Certificate caCertificate) {
    this.caCertificate = caCertificate;
  }

  /**
   * Загружает CA-сертификат из PEM/DER-файла
   *
   * @param caCertPath путь к {@code ca.crt}
   * @return якорь доверия
   */
  public static TrustAnchor loadFromFile(Path caCertPath) throws IOException {
    return new TrustAnchor(CertificateUtils.loadX509Certificate(caCertPath));
  }

  /**
   * Проверяет, что переданный сертификат сервера действительно подписан CA, на
   * текущий момент действителен и выдан именно для того хоста, к которому подключился клиент
   *
   * @param serverCertDer DER-байты сертификата сервера, полученные в {@code ServerHello}
   * @param expectedHostname хост, к которому подключался клиент
   * @return распаршенный и провалидированный сертификат сервера
   * @throws IOException если сертификат не парсится, не подписан CA, просрочен или CN не совпадает
   */
  public X509Certificate verifyServerCertificate(byte[] serverCertDer, String expectedHostname)
      throws IOException {
    X509Certificate serverCert = CertificateUtils.decodeCertificate(serverCertDer);
    try {
      serverCert.checkValidity();
      serverCert.verify(caCertificate.getPublicKey());
    } catch (GeneralSecurityException e) {
      throw new IOException(
          "Серверный сертификат не доверенный: "
              + e.getClass().getSimpleName()
              + ": "
              + e.getMessage(),
          e);
    }
    verifyHostname(serverCert, expectedHostname);
    return serverCert;
  }

  private static void verifyHostname(X509Certificate cert, String expectedHostname)
      throws IOException {
    if (matchesSubjectAlternativeNames(cert, expectedHostname)) {
      return;
    }
    String cn = extractCommonName(cert);
    if (cn != null && cn.equalsIgnoreCase(expectedHostname)) {
      return;
    }
    throw new IOException(
        "Сертификат сервера не подходит для хоста '"
            + expectedHostname
            + "' (ни SAN, ни CN не совпадают; CN="
            + cn
            + ")");
  }

  /**
   * Проверяет Subject Alternative Name. Возвращает {@code true}, если есть DNS- или IP-запись,
   * совпадающая с {@code expectedHostname}
   */
  private static boolean matchesSubjectAlternativeNames(X509Certificate cert, String expected)
      throws IOException {
    Collection<List<?>> sans;
    try {
      sans = cert.getSubjectAlternativeNames();
    } catch (CertificateParsingException e) {
      throw new IOException("Не удалось распарсить SAN сертификата", e);
    }
    if (sans == null) {
      return false;
    }
    for (List<?> entry : sans) {
      // entry = [type:Integer, value:Object]; type 2 = dNSName, 7 = iPAddress
      Integer type = (Integer) entry.get(0);
      String value = String.valueOf(entry.get(1));
      if ((type == SAN_TYPE_DNS || type == SAN_TYPE_IP) && value.equalsIgnoreCase(expected)) {
        return true;
      }
    }
    return false;
  }

  private static String extractCommonName(X509Certificate cert) throws IOException {
    String dn = cert.getSubjectX500Principal().getName();
    try {
      LdapName ldapName = new LdapName(dn);
      for (Rdn rdn : ldapName.getRdns()) {
        if ("CN".equalsIgnoreCase(rdn.getType())) {
          return String.valueOf(rdn.getValue());
        }
      }
      return null;
    } catch (InvalidNameException e) {
      throw new IOException("Не удалось распарсить Subject сертификата: " + dn, e);
    }
  }
}
