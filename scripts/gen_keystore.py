#!/usr/bin/env python3
"""Generate a PKCS12 keystore for signing the Hermes Android release APK.
Android's apksigner/jarsigner accept PKCS12 stores. Output is base64-printed
so it can be pasted as the GitHub repo secret KEYSTORE_BASE64."""
import base64, sys
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
import datetime

ALIAS = "hermes"
STORE_PASS = "hermes123"
KEY_PASS = "hermes123"

now = datetime.datetime.now(datetime.timezone.utc)

# RSA-2048 private key
key = rsa.generate_private_key(public_exponent=65537, key_size=2048)

# Self-signed cert (CN=Hermes), valid 10000 days
subject = issuer = x509.Name([
    x509.NameAttribute(NameOID.COMMON_NAME, "Hermes"),
    x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Android"),
    x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Nous Research"),
])
cert = (
    x509.CertificateBuilder()
    .subject_name(subject)
    .issuer_name(issuer)
    .public_key(key.public_key())
    .serial_number(x509.random_serial_number())
    .not_valid_before(now - datetime.timedelta(minutes=1))
    .not_valid_after(now + datetime.timedelta(days=10000))
    .sign(key, hashes.SHA256())
)

p12 = pkcs12.serialize_key_and_certificates(
    name=ALIAS.encode(),
    key=key,
    cert=cert,
    cas=None,
    encryption_algorithm=serialization.BestAvailableEncryption(STORE_PASS.encode()),
)

b64 = base64.b64encode(p12).decode()
with open(sys.argv[1] if len(sys.argv) > 1 else "/data/data/com.termux/files/home/hermes-release.p12", "wb") as f:
    f.write(p12)
with open("/data/data/com.termux/files/home/hermes-release.b64", "w") as f:
    f.write(b64)
print("WROTE keystore + base64 file. base64 length:", len(b64))
print("ALIAS=", ALIAS, "STORE_PASS=", STORE_PASS, "KEY_PASS=", KEY_PASS)
