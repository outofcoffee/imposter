
logger.debug("Calling ${url}")

def response = newRequest().url(url).method("GET").execute()
logger.debug("Response: ${response}")

respond().withContent(response.body)
