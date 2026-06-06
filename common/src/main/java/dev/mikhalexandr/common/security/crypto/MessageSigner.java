package dev.mikhalexandr.common.security.crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public final class MessageSigner {
  private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

  private MessageSigner() {
    throw new UnsupportedOperationException("Это утилитарный класс, его нельзя инстанцировать");
  }

  public static byte[] sign(byte[] data, PrivateKey privateKey) throws IOException {
    try {
      Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
      signature.initSign(privateKey);
      signature.update(data);
      return signature.sign();
    } catch (GeneralSecurityException e) {
      throw new IOException("Не удалось подписать данные RSA-SHA256", e);
    }
  }

  public static void verify(byte[] data, byte[] signatureBytes, PublicKey publicKey)
      throws IOException {
    try {
      Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
      signature.initVerify(publicKey);
      signature.update(data);
      if (!signature.verify(signatureBytes)) {
        throw new IOException(
            "Печаль, подпись невалидна, так как сервер не доказал владение приватным ключом");
      }
    } catch (GeneralSecurityException e) {
      throw new IOException("Печаль, не удалось проверить подпись RSA-SHA256", e);
    }
  }
}
