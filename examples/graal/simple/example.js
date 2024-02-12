const req = context.request;

if (req.queryParams.name) {
    respond()
        .withStatusCode(200)
        .withContent(`Hello ${req.queryParams.name}`);
} else {
    respond()
        .withStatusCode(400)
        .withContent('No name query parameter found');
}
