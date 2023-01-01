# SOAP request body matching example

This example uses an XPath expression to match a particular request body value and send a specific response.

Start mock server:

```bash
imposter up
```

Send request:

```bash
curl --data '<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <getPetByIdRequest xmlns="urn:com:example:petstore">
      <id>10</id>
    </getPetByIdRequest>
  </env:Body>
</env:Envelope>' http://localhost:8080/pets/
```

Response:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <getPetByIdResponse xmlns="urn:com:example:petstore">
        <id>10</id>
        <name>Whiskers</name>
    </getPetByIdResponse>
  </env:Body>
</env:Envelope>
```

## Notes

In order for the request matching to work, the namespace set in `imposter-config.yaml` *must* match the version of SOAP used in the request.

In this example, the following configuration snippet from the configuration file implies SOAP 1.2, due to the namespace used:

```yaml
xmlNamespaces:
  env: "http://www.w3.org/2003/05/soap-envelope"
  pets: "urn:com:example:petstore"
```

Therefore, in the request, the same namespace for the `soap:Envelope` element must be used:

```xml
<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <!-- etc. -->
</env:Envelope>
```
