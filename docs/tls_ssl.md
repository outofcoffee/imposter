# TLS/SSL

You can run Imposter with HTTPS enabled. To do this, enable the TLS option and provide keystore options.

*Note:* unless you explicitly override the listen port (`--listenPort`), enabling TLS will change the listen port to 8443.

### Example

    java -jar distro/all/build/libs/imposter-all.jar \
            --plugin rest \
            --configDir /path/to/config \
            --tlsEnabled \
            --keystorePath ./server/src/main/resources/keystore/ssl.jks \
            --keystorePassword password

**Note:** This example uses the self-signed certificate for TLS/SSL found in the source repository. You can, of course, use your own keystore instead. If you need to access the keys or certificate from this example, the keystore is located at `server/src/main/resources/keystore` and uses the secure password 'password'.
