// check bound variable
if ('world' == hello) {
    // respond with status code and file
    respond()
            .withStatusCode(201).and()
            .withFile("foo.bar")
            .withHeader("MyHeader", "AwesomeHeader")
            .immediately()
}
