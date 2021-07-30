logger.info('context: ' + context);

const request = context.request;

// Note: Graal.js doesn't have special Map accessor syntax:
// see: https://github.com/oracle/graaljs/issues/107
const quxParam = request.pathParams.get('qux');
const fooParam = request.queryParams.get('foo');
const bazHeader = request.headers.get('baz');
const corgeHeader = request.normalisedHeaders.get('corge');
const exampleEnvVar = env.get('example');

if (quxParam) {
    // echo the value of the 'qux' path parameter as a response header
    respond()
        .withStatusCode(203)
        .withHeader('X-Echo-Qux', quxParam);

} else if (fooParam) {
    // echo the value of the 'foo' request parameter as a response header
    respond()
        .withStatusCode(200)
        .withHeader('X-Echo-Foo', fooParam);

} else if (bazHeader) {
    // echo the value of the 'baz' request header as a response header
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Baz', bazHeader);

} else if (corgeHeader) {
    // echo the value of the 'corge' request header as a response header
    // note: the key is lowercase in normalisedHeaders, regardless of the request header casing
    respond()
        .withStatusCode(202)
        .withHeader('X-Echo-Corge', corgeHeader);

} else if (exampleEnvVar) {
    // echo the value of the 'example' environment variable as a response header
    respond()
        .withStatusCode(204)
        .withHeader('X-Echo-Env-Var', exampleEnvVar);

} else {
    // check bound variable
    if (hello === 'world') {
        // respond with status code and file
        respond()
            .withStatusCode(201).and()
            .withFile("foo.bar")
            .withHeader("MyHeader", "AwesomeHeader")
            .skipDefaultBehaviour();
    }
}
