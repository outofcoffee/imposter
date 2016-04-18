/**
 * Trivial example of returning a specific status code, based on the URI.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */

switch (scannerFilterPrefix) {
    case "fail":
        // HTTP Status-Code 400: Bad Request.
        respond() withStatusCode 400 immediately()
        return

}
