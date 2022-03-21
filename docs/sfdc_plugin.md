# SFDC (Salesforce) plugin

* Plugin name: `sfdc`
* Plugin class: `io.gatehill.imposter.plugin.sfdc.SfdcPluginImpl`

## Features

* Basic Salesforce mock implementation.
* Non-persistent SObject creation.
* Non-persistent SObject update.
* SObject retrieval by ID.
* Dummy SOQL queries.

## Install plugin

### Option 1: Using the CLI

To use this plugin, install it with the [Imposter CLI](./run_imposter_cli.md):

    imposter plugin install -d mock-sfdc

This will install the plugin version matching the current engine version used by the CLI. The next time you run `imposter up`, the plugin will be available.

### Option 2: Using the JAR

To use this plugin, download the plugin `imposter-plugin-mock-sfdc.jar` JAR file from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Enable it with the following environment variables:

    IMPOSTER_PLUGIN_DIR="/path/to/dir/containing/plugin"

## Using the plugin

> Read the [Configuration](configuration.md) section to understand how to configure Imposter.

**Note:** Clients interacting with this plugin often require TLS/SSL to be enabled. If this is the case for your client, ensure that you use an _https://_ scheme for accessing the mock server. See the TLS/SSL section in the [TLS/SSL](tls_ssl.md) section for more details.

**Note:** Typically, an OAuth2 login step precedes interactions with the SFDC API. As part of this, the mock server is required to return an `instance_url` to the client. Imposter uses the 'server URL', which is described in the [Usage](usage.md) section.

## Example

For working examples, see:

    mock/sfdc/src/test/resources/config

Let's assume your configuration is in a folder named `config`.

Docker example:

    docker run -ti -p 8080:8080 \
        -v $PWD/config:/opt/imposter/config \
        outofcoffee/imposter-all \
        --serverUrl http://localhost:8080

Standalone Java example:

    java -jar distro/sfdc/build/libs/imposter-all.jar \
        --configDir ./config \
        --serverUrl http://localhost:8080

This starts a mock server using the SFDC plugin. Responses are served based on the configuration files inside the `config` folder.

Using the example above, you can connect a Salesforce client, such as [ForceApi](https://github.com/jesperfj/force-rest-api), to [http://localhost:8080/](http://localhost:8080/) to interact with the API. In this example, you can interact with the `Account` SObject, as defined in `sfdc-plugin-config.json` and `sfdc-plugin-data.json`.

## Additional script context objects

There are no additional script context objects available.
