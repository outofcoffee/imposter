const {context, respond} = require("@imposter-js/types");

const exampleHeader = context.request.headers['X-Example'];

respond()
    .withStatusCode(201)
    .withData(exampleHeader);
