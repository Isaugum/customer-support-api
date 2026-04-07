#!/usr/bin/env bash
set -euo pipefail

KEY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "${KEY_DIR}/keys"

echo "🔑 Generating RSA-2048 key pair for JWT signing..."

openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -out "$KEY_DIR/keys/private.pem"
openssl rsa -pubout \
  -in "$KEY_DIR/keys/private.pem" \
  -out "$KEY_DIR/keys/public.pem"

chmod 644 "$KEY_DIR/keys/private.pem"
chmod 644 "$KEY_DIR/keys/public.pem"

echo "✅ Keys written to $KEY_DIR/keys"
echo "   private.pem — keep secret, used by the API to sign JWTs"
echo "   public.pem  — can be shared, used to verify JWTs"
echo ""
echo "⚠️  Make sure 'keys/' is in your .gitignore!"