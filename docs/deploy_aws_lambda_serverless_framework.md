# Deploy Imposter to AWS Lambda using Serverless Framework

This section describes how to deploy Imposter as an AWS Lambda function using [Serverless Framework](https://www.serverless.com).

> ### Other ways to deploy to Lambda
>
> You can also deploy Imposter as a Lambda function using the [Imposter CLI](deploy_aws_lambda_cli.md), infrastructure as code tools (e.g. Terraform) or the [AWS Console](./deploy_aws_lambda_console.md).

## Overview

The key steps are:

- you upload the Imposter configuration files to an S3 bucket
- you create a Serverless Framework configuration file referring to the Imposter Lambda ZIP file and configuration path in S3
- you deploy the function using the Serverless Framework
- you access the Lambda function via a Lambda Function URL or Amazon API Gateway

## Prerequisites

You must have an AWS account and permission to deploy Lambda functions, create/write to an S3 bucket, and configure API Gateway.

### Upload configuration to S3 bucket

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

---
## Deployment steps

This method uses the [Serverless Framework](https://www.serverless.com/framework) to create a Lambda function, and enables you to call it using the Lambda Function URL.

### Step 1: Install Serverless Framework

Install the Serverless Framework:

    npm install -g serverless

> See the [Serverless Framework getting started](https://www.serverless.com/framework/docs/getting-started) documentation.

### Step 2: Download the ZIP file

Download the `imposter-awslambda.zip` file from the [Releases page](https://github.com/outofcoffee/imposter/releases/latest).

### Step 3: Configure the function

Create the following configuration file, named `serverless.yml`:

```yaml
service: aws-imposter-example
frameworkVersion: '3'

provider:
  name: aws
  runtime: java11

  # permit the function to fetch the config from an S3 bucket named 'imposter-lambda-example'
  iamRoleStatements:
    - Effect: "Allow"
      Action: "s3:GetObject"
      Resource: "arn:aws:s3:::imposter-lambda-example/*"
    - Effect: "Allow"
      Action: "s3:ListBucket"
      Resource: "arn:aws:s3:::imposter-lambda-example"

package:
  individually: true

functions:
  imposter:
    handler: "io.gatehill.imposter.awslambda.HandlerV2"
    timeout: 30
    url: true
    package:
      artifact: "./imposter-awslambda.zip"
    environment:
      IMPOSTER_CONFIG_DIR: "s3://imposter-lambda-example/config/"
```

> Note: `IMPOSTER_CONFIG_DIR` is not the path to the YAML _file_ - it is the directory ('prefix') under which the file exists in the bucket.

*Important:* Ensure the Lambda execution role has permission to access the S3 bucket containing your configuration.

See [deploy/example/bucket-policy.json](https://raw.githubusercontent.com/outofcoffee/imposter/main/distro/awslambda/deploy/example/bucket-policy.json) for an example IAM role.

Deploy your Lambda function with the Serverless CLI:

    serverless deploy

### Step 4: Test the function

If all has gone well, you should be able to reach your Lambda function using the Function URL:

```shell
$ curl https://<Lambda Function URL>/system/status

{ "status": "ok" }
```

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
