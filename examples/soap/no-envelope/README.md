# SOAP plugin 'no envelope' example

By default, SOAP messages are wrapped in an `Envelope`. It is possible to disable the use of the SOAP `Envelope` in request/response messages.

Set `envelope: false` in the root of the configuration file, as shown in this example.

The request should then look like this:

```xml
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>3</id>
  <name>Paws</name>
</getPetByIdRequest>
```

and the response will be as follows:

```xml
<getPetByIdResponse xmlns="urn:com:example:petstore">
  <id>3</id>
  <name>Paws</name>
</getPetByIdResponse>
```

### Example

```shell
$ curl --data '<getPetByIdRequest xmlns="urn:com:example:petstore"><id>3</id></getPetByIdRequest>' http://localhost:8080/pets/
```
