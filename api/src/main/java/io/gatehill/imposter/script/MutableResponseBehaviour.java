package io.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface MutableResponseBehaviour extends ResponseBehaviour {
    MutableResponseBehaviour withHeader(String header, String value);

    MutableResponseBehaviour withStatusCode(int statusCode);

    MutableResponseBehaviour withFile(String responseFile);

    MutableResponseBehaviour withEmpty();

    MutableResponseBehaviour withData(String responseData);

    MutableResponseBehaviour withExampleName(String exampleName);

    MutableResponseBehaviour usingDefaultBehaviour();

    MutableResponseBehaviour immediately();

    MutableResponseBehaviour respond();

    MutableResponseBehaviour respond(Runnable closure);

    MutableResponseBehaviour and();
}
