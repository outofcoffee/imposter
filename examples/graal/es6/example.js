class Greeter {
    constructor(request) {
        this.request = request;
    }

    generateGreeting() {
        return `Hello ${this.request.queryParams.name}`;
    }
}

const calculateResponse = () => {
    const { request } = context;
    if (request.queryParams.name) {
        const greeter = new Greeter(request);
        return {
            statusCode: 200,
            content: greeter.generateGreeting(),
        };
    } else {
        return {
            statusCode: 400,
            content: 'No name query parameter found',
        };
    }
}

const { statusCode, content } = calculateResponse();

respond().withStatusCode(statusCode).withContent(content);
