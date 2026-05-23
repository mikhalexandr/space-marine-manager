package dev.mikhalexandr.server.network;

import dev.mikhalexandr.common.protocol.FrameCodec;
import dev.mikhalexandr.common.security.cert.CertificateUtils;
import dev.mikhalexandr.common.security.crypto.KeyAgreementService;
import dev.mikhalexandr.common.security.crypto.MessageSigner;
import dev.mikhalexandr.common.security.crypto.SessionCipher;
import dev.mikhalexandr.common.security.crypto.SessionKeys;
import dev.mikhalexandr.common.security.handshake.ClientHello;
import dev.mikhalexandr.common.security.handshake.HandshakeMessage;
import dev.mikhalexandr.common.security.handshake.ServerHello;
import dev.mikhalexandr.common.util.Bytes;
import dev.mikhalexandr.common.util.Serializer;
import dev.mikhalexandr.server.security.ServerIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.PublicKey;

final class ServerHandshake {
  private ServerHandshake() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  /**
   * Принимает {@code ClientHello}, отправляет {@code ServerHello} и согласует ключи сессии
   *
   * @param input входной поток сокета
   * @param output выходной поток сокета
   * @param identity серверная идентичность (сертификат + приватный ключ)
   * @return шифр согласованной сессии
   * @throws IOException если рукопожатие не удалось
   */
  static SessionCipher perform(InputStream input, OutputStream output, ServerIdentity identity)
      throws IOException {
    ClientHello hello = readClientHello(input);
    PublicKey clientEphemeral = KeyAgreementService.decodePublicKey(hello.ephemeralPublicKey());
    KeyPair serverEphemeral = KeyAgreementService.generateEphemeralKeyPair();
    byte[] serverEphemeralEncoded =
        KeyAgreementService.encodePublicKey(serverEphemeral.getPublic());

    byte[] transcript = Bytes.concat(hello.ephemeralPublicKey(), serverEphemeralEncoded);
    byte[] signature = MessageSigner.sign(transcript, identity.privateKey());
    byte[] sharedSecret =
        KeyAgreementService.computeSharedSecret(serverEphemeral.getPrivate(), clientEphemeral);
    byte[] transcriptHash =
        KeyAgreementService.transcriptHash(hello.ephemeralPublicKey(), serverEphemeralEncoded);
    SessionKeys keys = KeyAgreementService.deriveSessionKeys(sharedSecret, transcriptHash);

    ServerHello reply =
        new ServerHello(
            CertificateUtils.encodeCertificate(identity.certificate()),
            serverEphemeralEncoded,
            signature);
    FrameCodec.writeFrame(output, Serializer.serialize(reply));
    return new SessionCipher(keys.serverToClient(), keys.clientToServer());
  }

  private static ClientHello readClientHello(InputStream input) throws IOException {
    byte[] payload = FrameCodec.readFrame(input);
    try {
      return Serializer.deserialize(payload, HandshakeMessage.class).asClientHello();
    } catch (ClassNotFoundException e) {
      throw new IOException("Не удалось десериализовать handshake-фрейм", e);
    }
  }
}
