# Run Imposter with GitHub Actions

If you're using GitHub Actions for your CI/CD pipeline, you can run Imposter to mock your dependencies during testing.

<details markdown>
<summary>Other ways to run Imposter</summary>

**Standalone mock server**

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a Lambda function in AWS - see [Imposter AWS Lambda](./run_imposter_aws_lambda.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

**Embed in unit/integration tests**

- Embed within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
- Embed within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/imposter-project/imposter-js)

</details>

This guide will show you how to start and stop mocks with GitHub Actions.

## Available Actions

> **Note**
> Replace `v1` in the examples below with the [latest release](https://github.com/imposter-project/imposter-github-action/releases) of the GitHub Actions.

### 1. Setup Imposter (`setup`)
Downloads and installs the Imposter mock server.

```yaml
- uses: imposter-project/imposter-github-action/setup@v1
```

### 2. Start Mocks (`start-mocks`)
Starts the Imposter mock server in the background, and waits for it to be ready.

```yaml
- uses: imposter-project/imposter-github-action/start-mocks@v1
  with:
    # Optional: Path to the directory containing the Imposter configuration files
    config-dir: './mocks'      # default: './mocks'
    # Optional: Port number for the Imposter server
    port: '8080'               # default: '8080'
    # Optional: Version of the Imposter mock engine to use
    version: '1.2.3'           # default: '' (latest)
    # Optional: Type of mock engine to use (jvm or docker)
    engine-type: 'docker'      # default: 'docker'
    # Optional: Whether to recursively scan the config directory
    recursive-config-scan: 'false'  # default: 'false'
```

<details markdown>
<summary>Advanced configuration options</summary>

- `auto-restart`: Whether to automatically restart when configuration changes (default: false)
- `max-attempts`: Maximum number of attempts to check if the server is ready (default: 30)
- `retry-interval`: Interval in seconds between retry attempts (default: 1)

</details>

#### Outputs
- `base-url`: Base URL of the mock server (e.g. `http://localhost:8080`)

### 3. Stop Mocks (`stop-mocks`)
Stops the running Imposter mock server.

```yaml
- uses: imposter-project/imposter-github-action/stop-mocks@v1
  with:
    # Optional: Type of mock engine to use (jvm or docker)
    engine-type: 'docker'      # default: 'docker'
```

## Sample Workflow

Here's a complete example showing how to use all three actions in a workflow:

```yaml
name: Integration Tests with Mocks

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    # Install Imposter CLI
    - name: Setup Imposter
      uses: imposter-project/imposter-github-action/setup@v1
    
    # Start mock server
    - name: Start Mocks
      id: start-mocks
      uses: imposter-project/imposter-github-action/start-mocks@v1
      with:
        config-dir: './mocks'
        port: '8080'            # Optional: specify port number
        engine-type: 'docker'   # Optional: specify engine type
        version: '1.2.3'        # Optional: specify engine version
        recursive-config-scan: 'true'  # Optional: scan config directory recursively
    
    # Your test steps here
    - name: Run Tests
      run: |
        # The mock server is available at ${{ steps.start-mocks.outputs.base-url }}
        echo "Running tests against mock server at ${{ steps.start-mocks.outputs.base-url }}"
    
    # Stop mock server
    - name: Stop Mocks
      uses: imposter-project/imposter-github-action/stop-mocks@v1
      with:
        engine-type: 'docker'   # Should match the engine-type used in start-mocks
```

## Configuration

The mock server configuration should be placed in your repository according to the `config-dir` parameter (defaults to `./mocks`). For detailed information about configuring mocks, see the [configuration guide](./configuration.md).

## Further information

See the [imposter-github-action](https://github.com/imposter-project/imposter-github-action) project for further details.
