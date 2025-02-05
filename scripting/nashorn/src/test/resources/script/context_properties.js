var echo = 'request.method=' + context.request.method
echo += '\nrequest.method.json=' + JSON.stringify(context.request.method)
echo += '\nrequest.path=' + context.request.path

echo += '\nrequest.queryParams={}'
echo += '\nrequest.pathParams={}'

var header = context.request.headers.corge
echo += '\nrequest.headers={"corge":"' + header + '"}'
echo += '\nrequest.formParams={}'

respond().withContent(echo);
