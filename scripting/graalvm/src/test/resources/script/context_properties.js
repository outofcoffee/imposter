const echo = `request.method=${context.request.method}
request.method.json=${JSON.stringify(context.request.method)}
request.path=${context.request.path}
request.queryParams=${JSON.stringify(context.request.queryParams)}
request.pathParams=${JSON.stringify(context.request.pathParams)}
request.headers=${JSON.stringify(context.request.headers)}
request.formParams=${JSON.stringify(context.request.formParams)}
`

respond().withContent(echo);
