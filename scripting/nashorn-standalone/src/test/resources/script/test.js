logger.info('context: ' + context);
console.log('JS console is available');

var request = context.request;

if (request.pathParams.qux) {
    // echo the value of the 'qux' path parameter as a response header
    respond()
        .withStatusCode(203)
        .withHeader('X-Echo-Qux', request.pathParams.qux);

} else if (request.queryParams.foo) {
    // echo the value of the 'foo' request parameter as a response header
    respond()
        .withStatusCode(200)
        .withHeader('X-Echo-Foo', request.queryParams.foo);

} else if (request.headers.baz) {
    // echo the value of the 'baz' request header as a response header
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Baz', request.headers.baz);

} else if (request.normalisedHeaders.corge) {
    // echo the value of the 'corge' request header as a response header
    // note: the key is lowercase in normalisedHeaders, regardless of the request header casing
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Corge', request.normalisedHeaders.corge);

} else if (env.example) {
    // echo the value of the 'example' environment variable as a response header
    respond()
        .withStatusCode(204)
        .withHeader('X-Echo-Env-Var', env.example);

} else {
    // check bound variable
    if (hello === 'world') {
        // respond with status code and file
        respond()
            .withStatusCode(201).and()
            .withFile('foo.bar')
            .withHeader('MyHeader', 'AwesomeHeader')
            .skipDefaultBehaviour();
    }
}
