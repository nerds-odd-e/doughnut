#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd -P)"
CONFIG_PATH="${DATABASE_PRIVATE_IP_CONFIG_PATH:-$SCRIPT_DIR/../config/production-database-private-ip}"

if [[ ! -r "$CONFIG_PATH" ]]; then
  echo "Error: Production database private IP configuration is not readable at $CONFIG_PATH" >&2
  exit 1
fi

DATABASE_PRIVATE_IP=$(tr -d '[:space:]' <"$CONFIG_PATH")

is_ipv4_address() {
  local address=$1 octet
  local -a octets

  [[ "$address" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || return 1
  IFS=. read -r -a octets <<<"$address"
  for octet in "${octets[@]}"; do
    [[ "$octet" =~ ^(0|[1-9][0-9]{0,2})$ ]] || return 1
    ((10#$octet <= 255)) || return 1
  done
}

if ! is_ipv4_address "$DATABASE_PRIVATE_IP"; then
  echo "Error: Production database private IP configuration is invalid: <$DATABASE_PRIVATE_IP>" >&2
  exit 1
fi

printf '%s' "$DATABASE_PRIVATE_IP"
