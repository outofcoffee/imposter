def soapOperation = context.operation
logger.debug("Received SOAP request for operation: $soapOperation")

logger.debug("SOAP request: ${context.request.body}")

// determine response based on request
if (context.request.body.contains('bad-data-example')) {
    logger.debug('Bad data scenario - returning HTTP 400')
    respond().withStatusCode(400)
} else {
    respond().withFile('getPetByIdResponse.xml')
}
