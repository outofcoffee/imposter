// Example of returning a specific status code or response file,
// based on the scanner filter prefix (essentially the HBase query).

switch (context.responsePhase.toString()) {
    case "SCANNER":
        if ("fail" === context.scannerFilterPrefix) {
            // HTTP Status-Code 400: Bad Request.
            console.log("Matched 'fail' prefix - returning HTTP 400");
            respond()
                .withStatusCode(400)
                .skipDefaultBehaviour();
        }
        break

    case "RESULTS":
        console.log("Returning static results using default behaviour");
        respond()
            .withFile("hbase-data.json")
            .usingDefaultBehaviour();
        break
}
