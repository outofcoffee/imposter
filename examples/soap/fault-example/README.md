# Return a SOAP fault

This example returns a SOAP fault if the `id` in the request message is `10`.

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
<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
    <env:Header/>
    <env:Body>
        <urn:getPetFault xmlns:urn="urn:com:example:petstore">
            <code>3</code>
            <description>string</description>
        </urn:getPetFault>
    </env:Body>
</env:Envelope>
```

## Notes

This example uses conditional matching with resources to only return a fault under specific conditions.

If a non-matching `id` is sent in the request, the default response message is generated:

```bash
curl --data '<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
  <env:Header/>
  <env:Body>
    <getPetByIdRequest xmlns="urn:com:example:petstore">
      <id>3</id>
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
    <urn:getPetByIdResponse xmlns:urn="urn:com:example:petstore">
      <id>3</id>
      <name>string</name>
    </urn:getPetByIdResponse>
  </env:Body>
</env:Envelope>
```
