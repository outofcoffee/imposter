import com.gatehill.imposter.plugin.hbase.model.ResponsePhase

/**
 * Trivial example of returning a specific status code, based on the URI.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */

if (ResponsePhase.SCANNER.equals(context.responsePhase)) {
    switch (context.scannerFilterPrefix) {
        case "fail":
            // HTTP Status-Code 400: Bad Request.
            respond() withStatusCode 400 immediately()
            return

    }
}
