package dev.mikhalexandr.common.security.crypto;

import javax.crypto.SecretKey;

/** Пара симметричных ключей сессии: один для направления клиент -> сервер, второй наоборот */
public record SessionKeys(SecretKey clientToServer, SecretKey serverToClient) {}
