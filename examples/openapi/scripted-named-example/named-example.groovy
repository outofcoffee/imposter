
// Example of returning a specific example, based on the request URI.

if (context.request.pathParams.petId == '1') {
    respond().withExampleName('catExample')

} else if (context.request.pathParams.petId == '2') {
    respond().withExampleName('dogExample')
}
