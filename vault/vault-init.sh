#!/usr/bin/env bash

set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
CA_CN="${CA_CN:-my-ca}"
CA_TTL="${CA_TTL:-87600h}"
ROLE_NAME="${ROLE_NAME:-server-role}"
ROLE_MAX_TTL="${ROLE_MAX_TTL:-720h}"
ALLOWED_DOMAINS="${ALLOWED_DOMAINS:-localhost}"
APPROLE_NAME="${APPROLE_NAME:-server-app}"
APPROLE_TOKEN_TTL="${APPROLE_TOKEN_TTL:-1h}"
APPROLE_TOKEN_MAX_TTL="${APPROLE_TOKEN_MAX_TTL:-24h}"

CERT_DIR="${CERT_DIR:-client/certs}"
STATE_FILE="${STATE_FILE:-.vault-state.json}"
APPROLE_FILE="${APPROLE_FILE:-.vault-approle.env}"
POLICY_FILE="${POLICY_FILE:-vault/policies/server-policy.hcl}"

http() {
  local method="$1"
  local path="$2"
  shift 2
  curl --silent --show-error \
    -H "X-Vault-Token: ${VAULT_TOKEN:-}" \
    -H "Content-Type: application/json" \
    -X "$method" "$@" \
    "${VAULT_ADDR}/v1/${path}"
}

http_status() {
  local path="$1"
  curl --silent --output /dev/null --write-out '%{http_code}' \
    -H "X-Vault-Token: ${VAULT_TOKEN:-}" \
    "${VAULT_ADDR}/v1/${path}"
}

echo "Vault: ${VAULT_ADDR}"
if ! curl --silent --fail --max-time 3 "${VAULT_ADDR}/v1/sys/seal-status" >/dev/null 2>&1; then
  echo "Vault недоступен. Сначала: task vault:up" >&2
  exit 1
fi

INIT_STATUS=$(curl --silent "${VAULT_ADDR}/v1/sys/init")
INITIALIZED=$(echo "$INIT_STATUS" | python3 -c 'import sys,json;print(json.load(sys.stdin)["initialized"])')

if [ "$INITIALIZED" = "False" ]; then
  echo "Vault не инициализирован - запуск operator init"
  INIT_RESPONSE=$(curl --silent -X POST \
    -d '{"secret_shares": 1, "secret_threshold": 1}' \
    "${VAULT_ADDR}/v1/sys/init")
  echo "$INIT_RESPONSE" > "$STATE_FILE"
  chmod 600 "$STATE_FILE"
  echo "Сохранение unseal-key + root-token в $STATE_FILE"
else
  echo "Vault уже инициализирован, пропускаю init"
  if [ ! -f "$STATE_FILE" ]; then
    echo "  $STATE_FILE не найден, но Vault уже инициализирован" >&2
    exit 1
  fi
fi

UNSEAL_KEY=$(python3 -c 'import sys,json;print(json.load(open(sys.argv[1]))["keys"][0])' "$STATE_FILE")
VAULT_TOKEN=$(python3 -c 'import sys,json;print(json.load(open(sys.argv[1]))["root_token"])' "$STATE_FILE")
export VAULT_TOKEN

SEALED=$(curl --silent "${VAULT_ADDR}/v1/sys/seal-status" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["sealed"])')

if [ "$SEALED" = "True" ]; then
  echo "Vault запечатан"
  curl --silent -X PUT -d "{\"key\":\"${UNSEAL_KEY}\"}" \
    "${VAULT_ADDR}/v1/sys/unseal" >/dev/null
else
  echo "Vault уже unsealed, пропуск"
fi

if [ "$(http_status sys/mounts/pki)" != "200" ]; then
  echo "Вкл PKI secrets engine"
  http POST sys/mounts/pki \
    -d "{\"type\":\"pki\",\"config\":{\"max_lease_ttl\":\"${CA_TTL}\"}}" >/dev/null
  echo "==> Тюн max-lease-ttl=${CA_TTL}"
  http POST sys/mounts/pki/tune -d "{\"max_lease_ttl\":\"${CA_TTL}\"}" >/dev/null
  echo "==> Генерация root CA (CN=${CA_CN})"
  http POST pki/root/generate/internal \
    -d "{\"common_name\":\"${CA_CN}\",\"ttl\":\"${CA_TTL}\"}" >/dev/null
else
  echo "PKI engine уже включён, пропуск"
fi

echo "Создание/обновление роли ${ROLE_NAME}"
http POST "pki/roles/${ROLE_NAME}" -d "{
  \"allowed_domains\": \"${ALLOWED_DOMAINS}\",
  \"allow_subdomains\": true,
  \"allow_localhost\": true,
  \"allow_ip_sans\": true,
  \"max_ttl\": \"${ROLE_MAX_TTL}\",
  \"key_type\": \"rsa\",
  \"key_bits\": 2048
}" >/dev/null

if [ ! -f "$POLICY_FILE" ]; then
  echo "Файл политики не найдем $POLICY_FILE" >&2
  exit 1
fi
echo "Загрузка узкой policy server-policy из $POLICY_FILE"
POLICY_JSON=$(python3 -c 'import json,sys;print(json.dumps({"policy": open(sys.argv[1]).read()}))' "$POLICY_FILE")
http PUT sys/policies/acl/server-policy -d "$POLICY_JSON" >/dev/null

if [ "$(http_status sys/auth/approle)" != "200" ]; then
  echo "Вкл approle auth method"
  http POST sys/auth/approle -d '{"type":"approle"}' >/dev/null
else
  echo "approle уже включён"
fi

echo "Создание/обновление AppRole ${APPROLE_NAME}"
http POST "auth/approle/role/${APPROLE_NAME}" -d "{
  \"token_policies\": [\"server-policy\"],
  \"token_ttl\": \"${APPROLE_TOKEN_TTL}\",
  \"token_max_ttl\": \"${APPROLE_TOKEN_MAX_TTL}\",
  \"secret_id_ttl\": \"0\",
  \"secret_id_num_uses\": 0
}" >/dev/null

echo "Получение role_id"
ROLE_ID=$(http GET "auth/approle/role/${APPROLE_NAME}/role-id" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["role_id"])')

echo "Создание нового secret_id"
SECRET_ID=$(http POST "auth/approle/role/${APPROLE_NAME}/secret-id" -d '{}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["secret_id"])')

cat > "$APPROLE_FILE" <<EOF
VAULT_URL=${VAULT_ADDR}
VAULT_ROLE_ID=${ROLE_ID}
VAULT_SECRET_ID=${SECRET_ID}
VAULT_PKI_ROLE=${ROLE_NAME}
VAULT_COMMON_NAME=localhost
EOF
chmod 600 "$APPROLE_FILE"

echo "Экспорт публичного CA в ${CERT_DIR}/ca.crt"
mkdir -p "$CERT_DIR"
curl --silent --fail "${VAULT_ADDR}/v1/pki/ca/pem" -o "${CERT_DIR}/ca.crt"
