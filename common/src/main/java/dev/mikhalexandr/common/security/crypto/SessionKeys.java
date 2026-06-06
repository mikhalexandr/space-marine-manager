package dev.mikhalexandr.common.security.crypto;

import javax.crypto.SecretKey;

public record SessionKeys(SecretKey clientToServer, SecretKey serverToClient) {}
