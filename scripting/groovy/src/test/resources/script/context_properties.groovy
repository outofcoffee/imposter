import groovy.json.JsonOutput

def echo = """request.method=${context.request.method}
request.method.json=${JsonOutput.toJson(context.request.method)}
request.path=${context.request.path}
request.queryParams=${JsonOutput.toJson(context.request.queryParams)}
request.pathParams=${JsonOutput.toJson(context.request.pathParams)}
request.headers=${JsonOutput.toJson(context.request.headers)}
request.formParams=${JsonOutput.toJson(context.request.formParams)}
"""

respond().withContent(echo);
