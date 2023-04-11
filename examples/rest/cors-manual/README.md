# CORS example

In order for this example to work, you need to start two things: the static web server and the API server.

## API server

The API server uses Imposter. Start it on port 8080 with the following command:

    imposter up

## Static web server

The web server can use anything that can serve static content. Here's an example using Python:

    python3 -m http.server 8081

This starts the static web server on port 8081, so this page should be accessible at http://localhost:8081

## Test it

Open http://localhost:8081 in your browser. You should see a page with a button. Clicking the button should make a request to the API server and display the response.

Since the API server is hosted on a different origin (localhost:8081), the browser will block the request by default. The mock configuration for the API server has CORS headers configured to allow requests from any origin, so the request should succeed.
