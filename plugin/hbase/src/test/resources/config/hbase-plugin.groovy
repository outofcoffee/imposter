import com.gatehill.imposter.plugin.hbase.model.ResponsePhase

/**
 * Trivial example of returning a specific status code, based on the URI.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */

switch (context.responsePhase) {
    case ResponsePhase.SCANNER:
        if ("fail".equals(context.scannerFilterPrefix)) {
            // HTTP Status-Code 400: Bad Request.
            logger.info("Matched 'fail' prefix - returning HTTP 400")
            respond() withStatusCode 400 immediately()
        }
        break

    case ResponsePhase.RESULTS:
        logger.info("Returning static results using default behaviour")
        respond() withFile "hbase-data.json" withDefaultBehaviour()
        break
}
