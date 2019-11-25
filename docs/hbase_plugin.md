# HBase plugin

* Plugin name: `hbase`
* Plugin class: `io.gatehill.imposter.plugin.hbase.HBasePluginImpl`

## Features

* Basic HBase mock implementation.
* Supports protobuf or JSON for wire transport.
* Dummy Scanner queries.
* Individual table row/record retrieval.

# Configuration

Read the [Configuration](configuration.md) section to understand how to configure Imposter.

### Additional context objects

| Object | Type | Description
| --- | --- | ---
| `tableName` | `String` | The name of the HBase table.
| `responsePhase` | `io.gatehill.imposter.plugin.hbase.model.ResponsePhase` | The type of response being served.
| `scannerFilterPrefix` | `String` | The prefix from the filter of the result scanner.
| `recordInfo` | `io.gatehill.imposter.plugin.hbase.model.RecordInfo` | Information about the requested record, if a single record is requested.

## Using the plugin

**Note:** When using HBase Scanners, this plugin will return the 'server URL' in the `Location` header of the scanner creation response. You might want to consider setting the `serverUrl` property explicitly to the publicly-accessible address of the mock server, as described in the [Usage](usage.md) section.

## Example

For working examples, see:

    plugin/hbase/src/test/resources/config

Let's assume your configuration is in a folder named `config`.

Docker example:

    docker run -ti -p 8080:8080 \
        -v $(pwd)/config:/opt/imposter/config \
        outofcoffee/imposter-hbase \
        --serverUrl http://localhost:8080

Standalone Java example:

    java -jar distro/hbase/build/libs/imposter-hbase.jar \
        --configDir ./config \
        --serverUrl http://localhost:8080

This starts a mock server using the HBase plugin. Responses are served based on the configuration files
inside the `config` folder.

Using the example above, you can connect an HBase client, such as [Apache RemoteHTable](https://hbase.apache.org/0.94/apidocs/org/apache/hadoop/hbase/rest/client/RemoteHTable.html), to [http://localhost:8080/](http://localhost:8080/) to interact with the API. In this example, you can interact with the `exampleTable` table, as defined in `hbase-plugin-config.json` and `hbase-plugin-data.json`.
