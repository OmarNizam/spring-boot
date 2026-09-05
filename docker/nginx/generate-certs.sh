#!/usr/bin/env bash
# Generates a local self-signed certificate for the nginx reverse proxy used
# by `docker compose --profile full`. Dev/local use only — not a real CA cert.
#
# Output goes to docker/nginx/certs/ (gitignored); run this once per clone
# before `docker compose --profile full up --build`.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="$SCRIPT_DIR/certs"
CRT="$CERT_DIR/localhost.crt"
KEY="$CERT_DIR/localhost.key"

mkdir -p "$CERT_DIR"

if [[ -f "$CRT" && -f "$KEY" ]]; then
  echo "Certificate already exists at $CRT — skipping. Delete it first to regenerate."
  exit 0
fi

# -addext isn't available on LibreSSL (macOS system openssl), so the SAN is
# supplied via a generated config file instead — this form works on both
# LibreSSL and OpenSSL 3.x.
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$KEY" \
  -out "$CRT" \
  -days 365 \
  -subj "/CN=localhost" \
  -config <(cat <<'EOF'
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no

[req_distinguished_name]
CN = localhost

[v3_req]
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
EOF
) -extensions v3_req

chmod 600 "$KEY"

echo "Generated self-signed certificate:"
echo "  $CRT"
echo "  $KEY"
