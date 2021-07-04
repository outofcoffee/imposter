var testStore = stores.open('test');

switch (context.request.path) {
    case '/store':
        testStore.save('foo', context.request.queryParams.foo);
        respond().withStatusCode(201);
        break;

    case '/load':
        respond()
            .withStatusCode(200)
            .withHeader('Content-Type', 'text/plain')
            .withData(testStore.load('foo'));
        break;
}
