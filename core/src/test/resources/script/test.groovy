// check bound variable
if ('world' == hello) {
    // respond with status code and file
    respond()
            .withStatusCode(201).and()
            .withFile("foo.bar")
            .immediately()
            .withHeader("MyHeader", "AwesomeHeader")
}
