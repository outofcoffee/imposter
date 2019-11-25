// Example of returning a specific status code or response file,
// based on URI parameters or the absolute URI.
import io.vertx.core.http.HttpHeaders

switch (context.request.uri) {
    case ~/.*bad/:
        // HTTP Status-Code 400: Bad Request.
        respond {
            withStatusCode 400
            immediately()
        }
        break
    case ~/.*with-auth/:
        if (context.request.headers.get(HttpHeaders.AUTHORIZATION) == "AUTH_HEADER") {
            // HTTP Status-Code 204: No Content.
            respond {
                withStatusCode 204
                immediately()
            }
        } else {
            // HTTP Status-Code 400: Bad Request.
            respond {
                withStatusCode 400
                immediately()
            }
        }
        break

    default:
        switch (context.request.params.action) {
            case "create":
                // HTTP Status-Code 201: Created.
                respond {
                    withStatusCode 201
                    immediately()
                }
                break

            case "delete":
                // HTTP Status-Code 204: No Content.
                respond {
                    withStatusCode 204
                    immediately()
                }
                break

            case "fetch":
                // use a different static response file with the default behaviour
                respond {
                    withFile "rest-plugin-data2.json"
                    and()
                    usingDefaultBehaviour()
                }
                break

            default:
                // default to static file in config
                respond {
                    usingDefaultBehaviour()
                }
        }
}
