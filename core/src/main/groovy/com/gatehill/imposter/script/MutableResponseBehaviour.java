package com.gatehill.imposter.script;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface MutableResponseBehaviour {
    MutableResponseBehaviour withStatusCode(int statusCode);

    MutableResponseBehaviour withFile(String responseFile);

    MutableResponseBehaviour withEmpty();

    MutableResponseBehaviour usingDefaultBehaviour();

    MutableResponseBehaviour immediately();

    MutableResponseBehaviour respond();

    MutableResponseBehaviour respond(Runnable closure);

    MutableResponseBehaviour and();

    MutableResponseBehaviour withData(String responseData);
}
