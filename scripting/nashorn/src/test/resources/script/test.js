logger.info('context: ' + context);

var request = context.request;

if (request.params.foo) {
    // echo the value of the 'foo' request parameter as a response header
    respond()
        .withStatusCode(200)
        .withHeader('X-Echo-Foo', request.params.foo);

} else if (request.headers.baz) {
    // echo the value of the 'baz' request header as a response header
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Baz', request.headers.baz);

} else {
    // check bound variable
    if (hello === 'world') {
        // respond with status code and file
        respond()
            .withStatusCode(201).and()
            .withFile("foo.bar")
            .withHeader("MyHeader", "AwesomeHeader")
            .immediately();
    }
}
