# HBase plugin

Plugin class: `com.gatehill.imposter.plugin.hbase.HBasePluginImpl`

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
| `responsePhase` | `com.gatehill.imposter.plugin.hbase.model.ResponsePhase` | The type of response being served.
| `scannerFilterPrefix` | `String` | The prefix from the filter of the result scanner.
| `recordInfo` | `com.gatehill.imposter.plugin.hbase.model.RecordInfo` | Information about the requested record, if a single record is requested.

## Using the plugin

**Note:** This plugin will return the 'server URL' in the `Location` header of the scanner creation response. You might
want to consider setting the `serverUrl` property explicitly to the publicly-accessible address of the mock server,
as described in the [Usage](usage.md) section.

## Example

For working examples, see:

    plugin/hbase/src/test/resources/config
