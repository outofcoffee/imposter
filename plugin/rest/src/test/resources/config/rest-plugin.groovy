import com.gatehill.imposter.model.InvocationContext

/**
 * Trivial example of returning a specific status code, based on the URI.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */

// this assignment is only done for illustrative purposes
InvocationContext ctx = context

switch (ctx.uri) {
    case ~/.*bad/:
        // HTTP Status-Code 400: Bad Request.
        ctx.respondWithStatusCode 400
        return

}

switch (ctx.params["action"]) {
    case "create":
        // HTTP Status-Code 201: Created.
        ctx.respondWithStatusCode 201
        break

    case "delete":
        // HTTP Status-Code 204: No Content.
        ctx.respondWithStatusCode 204
        break

    default:
        // default to static file in config
        ctx.respondDefault()
}
