# Usage (command line arguments)

Some options can be controlled using command line arguments. These are described in this section.

## Command line arguments

The following command line arguments can be used:

     --configDir (-c) VAL   : Directory containing mock configuration files
     --help (-h)            : Display usage only
     --host (-b) VAL        : Bind host
     --keystorePassword VAL : Password for the keystore (default: password)
     --keystorePath VAL     : Path to the keystore (default: classpath:/keystore/ssl.jks)
     --listenPort (-l) N    : Listen port (default: 8080)
     --plugin (-p) VAL      : Plugin class name
     --serverUrl (-u) VAL   : Explicitly set the server address
     --tlsEnabled (-t)      : Whether TLS (HTTPS) is enabled (requires keystore to be configured) (default: false)
     --version (-v)         : Print version and exit

## Server URL

For some responses, such as from the [SFDC plugin](sfdc_plugin.md), Imposter uses the 'server URL',
which is computed automatically from the `host` and `listenPort` command line arguments. If this
is not the URL you wish to use, you can override this with the `serverUrl` command line argument.

## TLS/SSL

See [TLS/SSL](tls_ssl.md).
