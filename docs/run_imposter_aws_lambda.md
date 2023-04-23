# Running Imposter in AWS Lambda

There are many ways to run Imposter. This section describes how to run it as a Lambda function in AWS.

> ### Other ways to run Imposter
> 
> #### Standalone mock server
> 
> - Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
> - As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
> - As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)
> 
> #### Embedded in tests
> 
> - Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md)
> - Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

## AWS Lambda Features

- Access to mock server via Lambda function URL (or AWS API Gateway)
- Supports [OpenAPI](./openapi_plugin.md), [REST](./rest_plugin.md) and [SOAP](./soap_plugin.md) mocks
- Supports [Stores](./stores.md) for recording data for later use or review

## Deployment options

You can deploy Imposter as a Lambda function in various ways, such as the AWS Console, using infrastructure as code (e.g. Terraform) or a framework such as Serverless.

Another option is to use the Imposter CLI, which has some opinionated choices to make things easier. You should choose the option that best suits your environment.

### Deploy to Lambda using Imposter CLI

You can deploy as a Lambda function using the Imposter CLI.

#### 📖 [Deploy to Lambda using Imposter CLI](./deploy_aws_lambda_cli.md)

### Deploy to Lambda using AWS Console

You can also deploy as a Lambda function using the AWS Console.

#### 📖 [Deploy to Lambda using AWS Console](./deploy_aws_lambda_console.md)

### Deploy to Lambda with Serverless Framework

You can also deploy as a Lambda function using Serverless Framework.

#### 📖 [Deploy to Lambda with Serverless Framework](deploy_aws_lambda_serverless_framework.md).

### Other ways to deploy to Lambda

You can also deploy using infrastructure as code tools (e.g. Terraform or CloudFormation).

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
