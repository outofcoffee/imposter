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

## A note on certificates

SSL certificates must match the domain where you’re hosting an application, e.g. https://example.com would need a certificate issued for the example.com domain.

Normally, you would obtain a signed certificate for a given domain via a third party provider. Certificates signed by a public certificate authority (CA) will generally be trusted by client applications as the CA is trusted by your system.

If you generate a certificate yourself, or use the test one in this project, this is known as a self-signed certificate. Self-signed certificates are untrusted and you usually have to configure your client to accept them explicitly, such as via a trust store or other JVM-wide SSL configuration.
