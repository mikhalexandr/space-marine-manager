package dev.mikhalexandr.server.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mikhalexandr.common.security.cert.CertificateUtils;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP-клиент к HashiCorp Vault PKI engine
 *
 * <p>Логика "правильного" CSR-flow:
 *
 * <ol>
 *   <li>Сервер сам генерит RSA-2048 пару (приватный ключ остаётся локально, никогда не передаётся)
 *   <li>Строит PKCS#10 CSR через Bouncy Castle (Java JDK не умеет CSR из коробки)
 *   <li>Отправляет CSR в Vault: {@code POST /v1/pki/sign/{role}}
 *   <li>Vault подписывает CSR своим ca.key (который у Vault внутри, наружу не уезжает)
 *   <li>Возвращает PEM-сертификат, который мы парсим и используем
 * </ol>
 *
 * <p>Серверный приватный ключ существует только в памяти процесса; на диск не пишется.
 * Аутентификация в Vault - через AppRole (короткоживущий токен с узкой политикой) или прямым
 * токеном для dev-режима.
 */
public final class VaultPkiClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(VaultPkiClient.class);
  private static final String SIGN_ALGORITHM = "SHA256withRSA";
  private static final String KEY_ALGORITHM = "RSA";
  private static final int KEY_BITS = 2048;
  private static final int HTTP_TIMEOUT_SECONDS = 10;

  private final String vaultUrl;
  private final String vaultToken;
  private final String pkiRole;
  private final HttpClient http;

  private VaultPkiClient(String vaultUrl, String vaultToken, String pkiRole) {
    this.vaultUrl = stripTrailingSlash(vaultUrl);
    this.vaultToken = vaultToken;
    this.pkiRole = pkiRole;
    this.http =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS)).build();
  }

  /** Создаёт клиент, который ходит во Vault с готовым токеном (dev-режим) */
  public static VaultPkiClient withToken(String vaultUrl, String vaultToken, String pkiRole) {
    return new VaultPkiClient(vaultUrl, vaultToken, pkiRole);
  }

  /**
   * Создаёт клиент, который сам логинится в Vault через AppRole и получает узкий токен с
   * политикой server-policy
   */
  public static VaultPkiClient withAppRole(
      String vaultUrl, String roleId, String secretId, String pkiRole) throws IOException {
    String url = stripTrailingSlash(vaultUrl);
    LOGGER.info("Vault AppRole login: {}/v1/auth/approle/login (role_id={})", url, roleId);
    String token = approleLogin(url, roleId, secretId);
    return new VaultPkiClient(url, token, pkiRole);
  }

  private static String approleLogin(String vaultUrl, String roleId, String secretId)
      throws IOException {
    JsonObject body = new JsonObject();
    body.addProperty("role_id", roleId);
    body.addProperty("secret_id", secretId);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(vaultUrl + "/v1/auth/approle/login"))
            .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<String> response =
          HttpClient.newHttpClient()
              .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() / 100 != 2) {
        throw new IOException(
            "Vault AppRole login упал: HTTP "
                + response.statusCode()
                + " body="
                + response.body());
      }
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      JsonObject auth = json.getAsJsonObject("auth");
      if (auth == null || !auth.has("client_token")) {
        throw new IOException("Vault login вернул неожиданный JSON: " + response.body());
      }
      return auth.get("client_token").getAsString();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP-запрос AppRole login прерван", e);
    }
  }

  /**
   * Полный CSR-flow: генерит RSA-пару, строит CSR, шлёт во Vault, возвращает идентичность
   *
   * @param commonName CN сертификата
   * @return ServerIdentity с приватным ключом и подписанным сертификатом
   */
  public ServerIdentity provisionIdentity(String commonName) throws IOException {
    LOGGER.info("Vault PKI: запрашиваю серт для CN={} через {}/v1/pki/sign/{}",
        commonName, vaultUrl, pkiRole);
    KeyPair keyPair = generateKeyPair();
    String csrPem = buildCsrPem(keyPair, commonName);
    String certPem = signCsrViaVault(csrPem, commonName);
    X509Certificate cert = parseCertificate(certPem);
    LOGGER.info("Vault PKI: получен серт subject={}, issuer={}",
        cert.getSubjectX500Principal(), cert.getIssuerX500Principal());
    return ServerIdentity.of(keyPair.getPrivate(), cert);
  }

  private static KeyPair generateKeyPair() throws IOException {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
      generator.initialize(KEY_BITS);
      return generator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось сгенерировать RSA-пару для CSR", e);
    }
  }

  private static String buildCsrPem(KeyPair keyPair, String commonName) throws IOException {
    try {
      X500Name subject = new X500Name("CN=" + commonName);
      JcaPKCS10CertificationRequestBuilder builder =
          new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
      ContentSigner signer =
          new JcaContentSignerBuilder(SIGN_ALGORITHM).build(keyPair.getPrivate());
      PKCS10CertificationRequest csr = builder.build(signer);
      StringWriter writer = new StringWriter();
      try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
        pemWriter.writeObject(csr);
      }
      return writer.toString();
    } catch (OperatorCreationException e) {
      throw new IOException("Не удалось построить CSR", e);
    }
  }

  private String signCsrViaVault(String csrPem, String commonName) throws IOException {
    JsonObject body = new JsonObject();
    body.addProperty("csr", csrPem);
    body.addProperty("common_name", commonName);
    body.addProperty("format", "pem");
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(vaultUrl + "/v1/pki/sign/" + pkiRole))
            .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .header("X-Vault-Token", vaultToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response = sendRequest(request);
    if (response.statusCode() / 100 != 2) {
      throw new IOException(
          "Vault PKI sign упал: HTTP " + response.statusCode() + " body=" + response.body());
    }
    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
    JsonObject data = json.getAsJsonObject("data");
    if (data == null || !data.has("certificate")) {
      throw new IOException("Vault PKI sign вернул неожиданный JSON: " + response.body());
    }
    return data.get("certificate").getAsString();
  }

  private HttpResponse<String> sendRequest(HttpRequest request) throws IOException {
    try {
      return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP-запрос к Vault прерван", e);
    }
  }

  private static X509Certificate parseCertificate(String pem) throws IOException {
    return CertificateUtils.decodeCertificate(
        pemToDer(pem));
  }

  private static byte[] pemToDer(String pem) throws IOException {
    int start = pem.indexOf("-----BEGIN CERTIFICATE-----");
    int end = pem.indexOf("-----END CERTIFICATE-----");
    if (start < 0 || end < 0 || end <= start) {
      throw new IOException("Не найдены PEM-границы в ответе Vault");
    }
    String base64 = pem.substring(start + "-----BEGIN CERTIFICATE-----".length(), end).replaceAll("\\s+", "");
    return Base64.getDecoder().decode(base64);
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
