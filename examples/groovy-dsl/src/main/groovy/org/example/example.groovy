package org.example

import io.gatehill.imposter.script.ExecutionContext.Request

def req = context.request as Request

print("""Hello from ${req.path}""")
