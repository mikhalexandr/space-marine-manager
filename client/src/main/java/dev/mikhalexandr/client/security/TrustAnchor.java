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

public final class TrustAnchor {
  private static final int SAN_TYPE_DNS = 2;

  private static final int SAN_TYPE_IP = 7;

  private final X509Certificate caCertificate;

  private TrustAnchor(X509Certificate caCertificate) {
    this.caCertificate = caCertificate;
  }

  public static TrustAnchor loadFromFile(Path caCertPath) throws IOException {
    return new TrustAnchor(CertificateUtils.loadX509Certificate(caCertPath));
  }

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
