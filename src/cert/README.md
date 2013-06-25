keystore.jks contains a self-signed cert.
Alias: sign
Password: 123456
Validity: 9999

Full command:
keytool -genkeypair -keyalg EC -alias sign -keystore keystore.jks -storepass 123456 -validity 9999 -keysize 112

The weak algorithm and keysize is intentional to keep the result small.