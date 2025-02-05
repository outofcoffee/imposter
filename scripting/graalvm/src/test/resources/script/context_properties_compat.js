let echo = `request.method=${context.request.method}`
echo += `\nrequest.method.json=${JSON.stringify(context.request.method)}`
echo += `\nrequest.path=${context.request.path}`

echo += `\nrequest.queryParams=${JSON.stringify(context.request.queryParams)}`
echo += `\nrequest.pathParams=${JSON.stringify(context.request.pathParams)}`

const header = context.request.headers['corge']
echo += `\nrequest.headers={"corge":"${header}"}`
echo += `\nrequest.formParams=${JSON.stringify(context.request.formParams)}`

respond().withContent(echo);
