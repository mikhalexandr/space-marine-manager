package dev.mikhalexandr.server.security;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public final class ServerIdentity {
  private final PrivateKey privateKey;
  private final X509Certificate certificate;

  private ServerIdentity(PrivateKey privateKey, X509Certificate certificate) {
    this.privateKey = privateKey;
    this.certificate = certificate;
  }

  public static ServerIdentity of(PrivateKey privateKey, X509Certificate certificate) {
    return new ServerIdentity(privateKey, certificate);
  }

  public PrivateKey privateKey() {
    return privateKey;
  }

  public X509Certificate certificate() {
    return certificate;
  }
}
