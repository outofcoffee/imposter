
// Example of returning a specific example, based on the request URI.

if (context.request.uri.endsWith('/pets/1')) {
    respond().withExampleName('catExample')

} else if (context.request.uri.endsWith('/pets/2')) {
    respond().withExampleName('dogExample')
}
