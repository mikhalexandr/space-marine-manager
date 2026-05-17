package dev.mikhalexandr.server.security;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Криптографическая личность сервера: приватный ключ + сертификат, подписанный CA
 *
 * <p>Создаётся при старте сервера через {@link VaultPkiClient}: приватный ключ генерится локально
 * и существует только в памяти процесса, сертификат получается от Vault через CSR-подпись
 */
public final class ServerIdentity {
  private final PrivateKey privateKey;
  private final X509Certificate certificate;

  private ServerIdentity(PrivateKey privateKey, X509Certificate certificate) {
    this.privateKey = privateKey;
    this.certificate = certificate;
  }

  /**
   * Создаёт идентичность из готовой пары приватный ключ + сертификат
   *
   * @param privateKey приватный ключ сервера (в памяти, не на диске)
   * @param certificate сертификат, подписанный CA (приходит из Vault)
   */
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
