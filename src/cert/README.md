keystore.jks contains a self-signed cert.
Alias: sign
Password: 123456
Validity: 99999 days

Full command:
keytool -genkeypair -keyalg EC -alias sign -keystore keystore.jks -storepass 123456 -validity 99999 -keysize 112

The minimal algorithm and keysize is intentional to keep the result small, since we only want the functions not the security.