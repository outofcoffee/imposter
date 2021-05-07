logger.info('context: ' + context);

const request = context.request;

// Note: Graal.js doesn't have special Map accessor syntax:
// see: https://github.com/oracle/graaljs/issues/107
const fooParam = request.params.get('foo');
const bazHeader = request.headers.get('baz');

if (fooParam) {
    // echo the value of the 'foo' request parameter as a response header
    respond()
        .withStatusCode(200)
        .withHeader('X-Echo-Foo', fooParam);

} else if (bazHeader) {
    // echo the value of the 'baz' request header as a response header
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Baz', bazHeader);

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
