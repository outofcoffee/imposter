// Example of returning a specific status code, to control which
// specification example is returned in the response.

// applies to URIs ending with '/apis'
if (context.request.uri ==~ /(.*)\/apis$/) {

        // applies to PUT requests only
        switch (context.request.method) {
            case 'PUT':
                respond {
                    withStatusCode 201
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
