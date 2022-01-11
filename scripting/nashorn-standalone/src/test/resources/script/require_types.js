var imposter = require("@imposter-js/types");
var context = imposter.context;
var respond = imposter.respond;

var exampleHeader = context.request.headers['X-Example'];

respond()
    .withStatusCode(201)
    .withData(exampleHeader);
