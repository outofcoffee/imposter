import io.gatehill.imposter.plugin.hbase.model.ResponsePhase

// Example of returning a specific status code or response file,
// based on the scanner filter prefix (essentially the HBase query).

switch (context.responsePhase) {
    case ResponsePhase.SCANNER:
        if ("fail".equals(context.scannerFilterPrefix)) {
            // HTTP Status-Code 400: Bad Request.
            logger.info("Matched 'fail' prefix - returning HTTP 400")
            respond {
                withStatusCode 400
                immediately()
            }
        }
        break

    case ResponsePhase.RESULTS:
        logger.info("Returning static results using default behaviour")
        respond {
            withFile "hbase-data.json"
            usingDefaultBehaviour()
        }
        break
}
