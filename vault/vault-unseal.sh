#!/usr/bin/env bash

set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
STATE_FILE="${STATE_FILE:-.vault-state.json}"

if [ ! -f "$STATE_FILE" ]; then
  echo "Не нашёл $STATE_FILE" >&2
  exit 1
fi

SEALED=$(curl --silent --fail "${VAULT_ADDR}/v1/sys/seal-status" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["sealed"])')

if [ "$SEALED" = "False" ]; then
  echo "Vault уже unsealed"
  exit 0
fi

UNSEAL_KEY=$(python3 -c 'import sys,json;print(json.load(open(sys.argv[1]))["keys"][0])' "$STATE_FILE")

echo "==> Unseal Vault..."
curl --silent -X PUT -d "{\"key\":\"${UNSEAL_KEY}\"}" \
  "${VAULT_ADDR}/v1/sys/unseal" >/dev/null

echo "Vault распечатан"
