#!/usr/bin/env bash

# ca.key       приватный ключ CA
# ca.crt       публичный самоподписанный сертификат CA
# server.key   приватный ключ сервера
# server.csr   запрос на подписание
# server.crt   сертификат сервера, подписанный CA
# server.p12   PKCS12-keystore

set -euo pipefail

CERT_DIR="${CERT_DIR:-${1:-certs}}"
DAYS_CA="${DAYS_CA:-3650}"
DAYS_SERVER="${DAYS_SERVER:-365}"
KEYSIZE="${KEYSIZE:-2048}"
P12_PASS="${P12_PASS:-chuiz}"
SERVER_CN="${SERVER_CN:-localhost}"
SERVER_SAN="${SERVER_SAN:-DNS:localhost,IP:127.0.0.1,IP:::1}"
CA_CN="${CA_CN:-my-ca}"
ORG="${ORG:-SpaceMarineManager}"

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl не найден в PATH" >&2
  exit 1
fi

mkdir -p "$CERT_DIR"

echo "Очистка $CERT_DIR..."
rm -f "$CERT_DIR"/ca.key \
      "$CERT_DIR"/ca.crt \
      "$CERT_DIR"/ca.srl \
      "$CERT_DIR"/server.key \
      "$CERT_DIR"/server.csr \
      "$CERT_DIR"/server.crt \
      "$CERT_DIR"/server.p12

cd "$CERT_DIR"

echo "Генерация ca.key..."
openssl genrsa -out ca.key "$KEYSIZE" 2>/dev/null

echo "Создание ca.crt..."
openssl req -x509 -new -key ca.key -out ca.crt -days "$DAYS_CA" \
  -subj "/CN=${CA_CN}/O=${ORG}" 2>/dev/null

echo "Генерация server.key..."
openssl genrsa -out server.key "$KEYSIZE" 2>/dev/null

echo "Создание server.csr..."
openssl req -new -key server.key -out server.csr \
  -subj "/CN=${SERVER_CN}/O=${ORG}" 2>/dev/null

echo "Подписывание server.crt c SAN=${SERVER_SAN}..."
openssl x509 -req \
  -in server.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial \
  -out server.crt -days "$DAYS_SERVER" \
  -extfile <(printf "subjectAltName=%s\n" "$SERVER_SAN") 2>/dev/null

echo "Упаковка ключа+цепочки в PKCS12 (server.p12, alias=server)..."
openssl pkcs12 -export \
  -inkey server.key \
  -in server.crt \
  -certfile ca.crt \
  -out server.p12 \
  -name server \
  -passout pass:"$P12_PASS"

echo
echo "Файлы в $(pwd):"
ls -la

echo
echo "CA-сертификат:"
openssl x509 -in ca.crt -noout -subject -issuer -dates

echo
echo "Серверный сертификат:"
openssl x509 -in server.crt -noout -subject -issuer -dates

echo
echo "Готово. PKCS12-пароль: $P12_PASS"
