# SOAP plugin

* Plugin name: `soap`
* Plugin class: `io.gatehill.imposter.plugin.soap.SoapPluginImpl`

This plugin provides support for SOAP web services, via WSDL files.

## Features

* Creates mock SOAP web service endpoints from WSDL 1.x and WSDL 2.0 files.
* Generates responses based on the schema/XSD of the web service invoked.
* Supports custom responses, headers, status codes etc. based on SOAP operation/binding/action and more.
* Also supports script-driven responses for maximum control, in either JavaScript or Groovy.

## Using the plugin

A great way to use this plugin is to take advantage of the request/response types defined in the WSDL types or referenced XSD files. These allow the mock server to generate a response without custom configuration or code, just by parsing the schema of the response message specified in the WSDL types/XSD.

This plugin will match the operation using a combination of:

* matching URI/path
* matching HTTP method
* matching SOAPAction (if required)
* matching XML schema type of the root element within the request SOAP envelope body 

Imposter will return the first response found that matches the above criteria. You can, of course, override the behaviour by setting the response body (see below) or status code.

Typically, you will use the configuration file `<something>-config.yaml` to customise the response, however, you can use the in-built script engine to gain further control of the response data, headers etc.

## Example

Here is an example configuration file:

```yaml
# petstore-config.yaml
---
plugin: soap
wsdlFile: petstore.wsdl
```

In this example, we are using a WSDL file (`petstore.wsdl`) containing the following service:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<description xmlns="http://www.w3.org/ns/wsdl"
             xmlns:tns="urn:com:example:petstore"
             xmlns:wsoap="http://www.w3.org/ns/wsdl/soap"
             targetNamespace="urn:com:example:petstore" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://www.w3.org/ns/wsdl/soap http://www.w3.org/2002/ws/desc/ns/soap.xsd">

    <documentation>
        This is a sample WSDL 2.0 document describing the pet service.
    </documentation>

    <types>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                   xmlns="urn:com:example:petstore"
                   targetNamespace="urn:com:example:petstore">

            <xs:import namespace="urn:com:example:petstore"
                       schemaLocation="schema.xsd"/>
        </xs:schema>
    </types>

    <interface name="PetInterface">
        <fault name="Error1" element="tns:fault"/>

        <operation name="getPetById" pattern="http://www.w3.org/ns/wsdl/in-out">
            <wsoap:operation soapAction="getPetById" style="document"/>
            <input messageLabel="In" element="tns:getPetByIdRequest"/>
            <output messageLabel="Out" element="tns:getPetByIdResponse"/>
        </operation>
    </interface>

    <binding name="SoapBinding" interface="tns:PetInterface"
             type="http://www.w3.org/ns/wsdl/soap"
             wsoap:protocol="http://www.w3.org/2003/05/soap/bindings/HTTP/"
             wsoap:mepDefault="http://www.w3.org/2003/05/soap/mep/request-response">
        <operation ref="tns:getPetById"/>
    </binding>

    <service name="PetService" interface="tns:PetInterface">
        <endpoint name="SoapEndpoint"
                  binding="tns:SoapBinding"
                  address="http://www.example.com/pets/"/>
    </service>
</description>
```

Some highlights:

* We’ve defined the service `PetService` at the SOAP endpoint `/pets/`
* We’ve said it has one operation: `getPetById`
* A request and response ('input' and 'output') message is defined using an external schema (`schema.xsd`)

### Start Imposter with the SOAP plugin

> The SOAP plugin is bundled with the core Imposter distribution.

Let's assume your configuration is in the directory: `examples/soap/simple`. Here are a few ways to start a mock running on port 8080.

CLI example:

    imposter up -p 8080 ./examples/soap/simple

Docker example:

    docker run --rm -ti -p 8080:8080 \
        -v $PWD/examples/soap/simple:/opt/imposter/config \
        outofcoffee/imposter-all

Java JAR example:

    java -jar distro/soap/build/libs/imposter-all.jar \
        --configDir ./examples/soap/simple

This starts a mock server using the SOAP plugin. Responses are served based on the WSDL file and its referenced XSD file `schema.xsd`.

Using the example above, you can interact with the APIs in the SOAP web service at their respective endpoints under `http://localhost:8080/pets/`.

Send a SOAP request to the `/pets/` path defined in the configuration file to see the example response:

```shell
$ curl -X POST "http://localhost:8080/pets/" \
    -H 'Content-Type: application/soap+xml' \
    -d '<?xml version="1.0" encoding="UTF-8"?>
        <env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
            <env:Header/>
            <env:Body>
                <getPetByIdRequest xmlns="urn:com:example:petstore">
                    <id>3</id>
                </getPetByIdRequest>
            </env:Body>
        </env:Envelope>'
```
> Tip: don't forget the trailing quote at the end of the string!

Response:
```shell
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
    <env:Header/>
    <env:Body>
        <getPetByIdResponse xmlns="urn:com:example:petstore">
            <id>3</id>
            <name>string</name>
        </getPetByIdResponse>
    </env:Body>
</env:Envelope>
```

It works! Your mock is returning an example SOAP response at the path `/pets/`, all from Imposter understanding the WSDL file.

Once you're finished, stop the server with CTRL+C.

> For more working examples, see:
>
> * [examples/soap](https://github.com/outofcoffee/imposter/blob/main/examples/soap)

## Conditional responses

You can control response behaviour based on the value of the request path, SOAP binding, SOAP operation, SOAPAction or body content.

```yaml
# custom-response-config.yaml
---
plugin: soap
wsdlFile: petstore.wsdl

resources:
  # return custom response body for a given operation
  - path: "/pets/"
    operation: getPetById
    response:
      file: getPetByIdResponse.xml
      
  # return HTTP 400 response if SOAPAction matches a specific value
  - path: "/pets/"
    soapAction: "invalid-pet-action"
    response:
      statusCode: 400
```

### First example
Here we expect the content of the file: `getPetByIdResponse.xml`.

```shell
$ curl -v -X POST http://localhost:8080/pets/ \
    -H 'Content-Type: application/soap+xml' \
    -d '<?xml version="1.0" encoding="UTF-8"?>
        <env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
            <env:Header/>
            <env:Body>
                <getPetByIdRequest xmlns="urn:com:example:petstore">
                    <id>3</id>
                </getPetByIdRequest>
            </env:Body>
        </env:Envelope>'
```

Response:
```shell
HTTP/1.1 200 OK
...
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
    <env:Header/>
    <env:Body>
        <getPetByIdResponse xmlns="urn:com:example:petstore">
            <id>3</id>
            <name>Custom pet name</name>
        </getPetByIdResponse>
    </env:Body>
</env:Envelope>
```

### Second example
Here we expect an HTTP 400 status code, given the matching SOAPAction.

```shell
$ curl -v -X POST http://localhost:8080/pets/ -H 'SOAPAction: invalid-pet-action'
HTTP/1.1 400 Bad Request
```

## Returning fault messages

If your WSDL document defines a `fault`, then Imposter can generate a sample response from its type.

To return a fault you can:

1. set the response status code to `500`, or
2. set the `response.soapFault` configuration property to `true`, or
3. use the `respond().withSoapFault()` script function

### Example configuration to respond with a fault

```yaml
plugin: soap
wsdlFile: service.wsdl

resources:
  - binding: SoapBinding
    operation: getPetById
    response:
      statusCode: 500
```

> **Tip**
> Use conditional matching with resources, to only return a fault in particular circumstances.
> See [fault-example](https://github.com/outofcoffee/imposter/blob/main/examples/soap/fault-example) for an example of how to do this.

## Scripted responses (advanced)

For more advanced scenarios, you can also control Imposter's responses using JavaScript or Groovy scripts.

> See the [Scripting](scripting.md) section for more information.

For a simple script, see [examples/soap/scripted-example](https://github.com/outofcoffee/imposter/blob/main/examples/soap/scripted-example) for a working example.

### Additional context objects

This plugin adds objects to the script `context`:

| Object       | Type                                                   | Description                                                 |
|--------------|--------------------------------------------------------|-------------------------------------------------------------|
| `binding`    | `io.gatehill.imposter.plugin.soap.model.WsdlBinding`   | The SOAP binding for the request.                           |
| `operation`  | `io.gatehill.imposter.plugin.soap.model.WsdlOperation` | The SOAP operation for the request.                         |
| `soapAction` | `String?`                                              | The SOAPAction from the request, if supplied by the client. |

### Example

Here we set the `response.scriptFile` property in the configuration file:

```yaml
# scripted-soap-config.yaml
---
plugin: soap
wsdlFile: petstore.wsdl
response:
  scriptFile: example.groovy
```

> As a reminder, you can use either JavaScript (`.js`) or Groovy (`.groovy`) languages for your scripts.

Now, `example.groovy` can control the responses, such as:

#### Return the content of a file

```groovy
respond().withFile('some-file.xml')
```

#### Return a literal string

```groovy
respond().withContent('''<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2001/12/soap-envelope">
 <env:Header/>
 <env:Body>
     <getPetByIdResponse xmlns="urn:com:example:petstore">
         <id>3</id>
         <name>Custom pet name</name>
     </getPetByIdResponse>
 </env:Body>
</env:Envelope>
''')
```

#### Return a specific HTTP status code

Setting the status code to 500 will trigger a fault message to be returned if one is defined within the WSDL document.

```groovy
respond().withStatusCode(500)
```

#### Scripting examples

- [conditional-example](https://github.com/outofcoffee/imposter/blob/main/examples/soap/conditional-example)
- [scripted-example](https://github.com/outofcoffee/imposter/blob/main/examples/soap/scripted-example)
- [fault-example](https://github.com/outofcoffee/imposter/blob/main/examples/soap/fault-example)

### Configuration reference

In addition to the standard [configuration](./configuration.md) file options, the following additional properties are supported.

| Configuration name | Purpose                                                  | Default value |
|--------------------|----------------------------------------------------------|---------------|
| `path`             | (Optional) A string to prepend to each operation's path. | Empty         |
| `wsdlFile`         | (Required) path to WSDL file (see below).                | Empty         |

#### WSDL file locations

WSDL files are provided as a relative path, using the directory containing the referencing configuration file as a base.

Some examples:

A file in the same directory as the configuration file:

```yaml
plugin: soap
wsdlFile: sample_service.wsdl
```

A file in a subdirectory relative to the configuration file:

```yaml
plugin: soap
wsdlFile: ./services/sample_service.wsdl
```

#### XSD file locations

XSD files are often used to describe the schema used by the operations in the WSDL file. When parsing a WSDL file, Imposter loads the content of all XSD files in the same directory as the WSDL file. This means that all XSD types defined in those files are used to support resolution of types referenced from the WSDL file.

For example:

```shell
$ ls
imposter-config.yaml  service.wsdl  schema.xsd  another_schema.xsd
```

If the `imposter-config.yaml` file is as follows, then both XSD files will be read to build the schema for the mock SOAP service.

```yaml
plugin: soap
wsdlFile: service.wsdl
```
