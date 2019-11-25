if (context.request.uri ==~ /(.*)\/apis\/v1beta1\/runs\/.+\/nodes\/.+\/artifacts\/.+/) {
    respond()
            .withStatusCode(200)
            .withHeader('Content-Type', 'application/octet-stream')
            .withData('Example artifact data')
} else {
    logger.info("No behaviour configured for: ${context.request.uri}")
}
