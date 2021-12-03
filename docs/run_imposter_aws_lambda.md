# Running Imposter in AWS Lambda

There are many ways to run Imposter. This section describes how to run it as a Lambda function in AWS.

---
### Other ways to run Imposter

#### Standalone mock server

- Using the command line client - see [Imposter CLI](./run_imposter_cli.md)
- As a Docker container - see [Imposter Docker container](./run_imposter_docker.md)
- As a JAR file on the JVM - see [Imposter JAR file](./run_imposter_jar.md)

#### Embedded in tests

- Embedded within your **Java/Kotlin/Scala/JVM** unit tests - see [JVM bindings](./embed_jvm.md) 
- Embedded within your **JavaScript/Node.js** unit tests - see [JavaScript bindings](https://github.com/gatehill/imposter-js)

---

## AWS Lambda Features

- Start mocks
- Access mock server via AWS API Gateway
- Supports [OpenAPI](./openapi_plugin.md) and [REST](./rest_plugin.md)

## Run

You can deploy Imposter as a [Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/java-package.html) in various ways, such as using the AWS Console, infrastructure as code (e.g. Terraform) or a framework such as Serverless.

The key concepts are:

- you upload the Imposter JAR file and your configuration files to an S3 bucket
- you create a Lambda function using the JAR file as the code source
- you set the environment variables of the function to refer to the configuration path in S3
- you access the Lambda function via Amazon API Gateway, using the proxy integration

### Prerequisites

You must have an AWS account and permission to deploy Lambda functions, create/write to an S3 bucket, and configure API Gateway.

#### Upload configuration to S3 bucket

We are going to store the Imposter configuration files in an S3 bucket. Upload your configuration files to the S3 bucket, under a path such as `config`.

For the purposes of this guide, we will assume you have uploaded the configuration to a bucket named `example-imposter-bucket`, so the full path to the configuration file would be:

```
s3://example-imposter-bucket/config/imposter-config.yaml
```

You may add related files here, such as response files, specifications etc.

```
s3://example-imposter-bucket/config/openapi-spec.yaml
s3://example-imposter-bucket/config/response.json
...
```

### Option A: Using Serverless framework

#### Step 1: Install Serverless framework

Install the [Serverless framework](https://www.serverless.com/framework/docs/getting-started):

    npm install -g serverless

#### Step 2: Download the JAR file

Download the `imposter-awslambda.jar` file from the [Releases page](https://github.com/outofcoffee/imposter/releases/latest).

#### Step 3: Configure the function 

Use the following configuration file:

```yaml
service: aws-imposter-example
frameworkVersion: '2'

provider:
  name: aws
  runtime: java8

package:
  individually: true

functions:
  imposter:
    handler: io.gatehill.imposter.awslambda.Handler
    timeout: 30
    package:
      artifact: ./imposter-awslambda.jar
    environment:
      IMPOSTER_S3_CONFIG_URL: "s3://example-imposter-bucket/config/"
    events:
      - http:
          method: any
          path: /{proxy+}
```

> Note: `IMPOSTER_S3_CONFIG_URL` is not the path to the YAML _file_ - it is the directory ('prefix') under which the file exists in the bucket.

*Important:* Ensure the Lambda execution role has permission to access the S3 bucket containing your configuration.

See [deploy/example/bucket-policy.json](https://raw.githubusercontent.com/outofcoffee/imposter/distro/awslambda/deploy/example/bucket-policy.json) for an example IAM role.

Deploy your Lambda function with the Serverless CLI:

    serverless deploy

#### Step 4: Call the function

Invoke the Imposter function:

    curl https://<API Gateway URL>/system/status

### Option B: Using the AWS Web Console

#### Step 1: Upload Imposter to an S3 bucket

The Imposter engine for AWS Lambda is packaged as a JAR file. You should upload this file to an S3 bucket, from where it will be referenced by your Lambda function.

Open the [AWS S3 Console](https://s3.console.aws.amazon.com/s3/home). Upload the `imposter-awslambda.jar` file from the [Releases page](https://github.com/outofcoffee/imposter/releases/latest) to an S3 bucket.

For the purposes of this guide, we will assume you have uploaded the JAR file to a bucket named `example-imposter-bucket`, so the full path to the file would be:

```
s3://example-imposter-bucket/imposter-awslambda.jar
```

#### Step 2: Create your Lambda function

Open the [AWS Lambda Console](https://eu-west-1.console.aws.amazon.com/lambda/home). Create a new function using the Java 8 runtime, using the file you uploaded to S3 as the code source.

*Important:* Set the handler to `io.gatehill.imposter.awslambda.Handler`

Set the environment variable to point to the path holding the configuration files:

```
IMPOSTER_S3_CONFIG_URL="s3://example-imposter-bucket/config/"
```

> Note: this is not the path to the YAML _file_ - it is the directory ('prefix') under which the file exists in the bucket.

*Important:* Ensure the Lambda execution role has permission to access the S3 bucket containing your configuration.

See [deploy/example/bucket-policy.json](https://raw.githubusercontent.com/outofcoffee/imposter/distro/awslambda/deploy/example/bucket-policy.json) for an example IAM role.

#### Step 3: Expose your function via API Gateway

Open the [AWS API Gateway console](https://console.aws.amazon.com/apigateway/home). Create a new API with an integration using the Lambda created above.

#### Step 4: Call the function

Invoke the Imposter function:

    curl https://<API Gateway URL>/system/status

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
