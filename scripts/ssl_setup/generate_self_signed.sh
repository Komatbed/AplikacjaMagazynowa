#!/bin/bash

# Configuration
SSL_DIR="nginx/ssl"
CERT_NAME="nginx-selfsigned"
COUNTRY="PL"
STATE="Mazowieckie"
CITY="Warsaw"
ORG="WarehouseApp"
OU="IT"
CN="51.77.59.105" # IP of the VPS

# Create SSL directory if not exists
if [ ! -d "$SSL_DIR" ]; then
    echo "Creating SSL directory: $SSL_DIR"
    mkdir -p "$SSL_DIR"
fi

# Generate Self-Signed Certificate
echo "Generating Self-Signed Certificate..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$SSL_DIR/$CERT_NAME.key" \
    -out "$SSL_DIR/$CERT_NAME.crt" \
    -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORG/OU=$OU/CN=$CN"

# Set permissions
chmod 644 "$SSL_DIR/$CERT_NAME.crt"
chmod 600 "$SSL_DIR/$CERT_NAME.key"

echo "Certificate generated successfully!"
echo "Key: $SSL_DIR/$CERT_NAME.key"
echo "Cert: $SSL_DIR/$CERT_NAME.crt"
