import io.vertx.core.http.HttpHeaders

// Example of returning a specific status code, to control which
// specification example is returned in the response.

// checks for a particular parameter
logger.info("Request params: ${context.request.params}")
if (context.request.params.param1 == 'foo') {
    respond {
        withStatusCode 202
        immediately()
    }
}

// check for deprecated params map
if (context.params.param1 == 'bar') {
    respond {
        withStatusCode 202
        immediately()
    }
}

// check for deprecated uri property
if (context.uri == 'baz') {
    respond {
        withStatusCode 400
        immediately()
    }
}

// applies to URIs ending with '/apis'
if (context.request.uri ==~ /(.*)\/apis$/) {

    // applies to PUT requests only
    switch (context.request.method) {
        case 'PUT':
            respond {
                withStatusCode 201
                withHeader("MyHeader", "MyHeaderValue")
            }
            break
        case 'GET':
            if (context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
                respond {
                    withStatusCode 204
                }
            } else {
                respond {
                    usingDefaultBehaviour()
                }
            }
            break
        default:
            // fallback to specification examples
            respond {
                usingDefaultBehaviour()
            }
            break
    }
}
