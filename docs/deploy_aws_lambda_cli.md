# Deploy Imposter to AWS Lambda using the CLI

This section describes how to deploy Imposter as an AWS Lambda function using Imposter CLI.

> ### Other ways to deploy to Lambda
> 
> You can also deploy Imposter as a Lambda function using the [AWS Console](deploy_aws_lambda_console.md), infrastructure as code tools (e.g. Terraform) or a framework such as [Serverless](deploy_aws_lambda_serverless_framework.md).

## Overview

The key steps are:

- use the Imposter CLI to configure AWS Lambda as the remote deployment target
- use the Imposter CLI to deploy your configuration (bundled with the Imposter engine) as a Lambda function to the desired AWS region
- access the Lambda function via a Lambda Function URL

## Prerequisites

- You must have an AWS account and permission to deploy Lambda functions, fetch an IAM role (and optionally create a role if not using an existing role).
- You must install the [Imposter CLI](./run_imposter_cli.md).

## Deployment steps

For the purposes of this guide, we will assume your working directory contains an Imposter configuration file.

```shell
$ ls -l
mock-config.yaml   response.txt
```

> **Note**
> See the [Configuration section](./configuration.md) for more information.

#### Step 1: Prepare your workspace

To begin, create a _workspace_ in this directory:

```shell
$ imposter workspace new example

created workspace 'example'
```

A workspace holds configuration, such as details of the remote deployment.

You can always check the active workspace by running the `imposter workspace show` command:
```shell
$ imposter workspace show

active workspace: example
```

> You can commit your workspace directory to your source control system (by default, stored under the `.imposter` subdirectory).

#### Step 2: Configure the remote

A workspace has a 'remote', where Imposter will be deployed. Set the remote type to AWS Lambda:

```shell
$ imposter remote set-type awslambda

set remote type to 'awslambda' for remote: example
```

Once you have the remote type, you can further configure the remote using the `imposter remote config` command.

For example:

```shell
$ imposter remote config region=eu-west-1
```

Available configuration options are:

| Property        | Meaning                                                                                                                                                                                                 | Default                                                                        |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| `anonAccess`    | Whether to permit anonymous access to the Lambda function URL (`true` or `false`)                                                                                                                       | `false`                                                                        |
| `architecture`  | The lambda architecture, which can be `x86_64` or `arm64`                                                                                                                                               | `x86_64`                                                                       |
| `engineVersion` | The version of the Imposter engine to use, such as `3.2.1`                                                                                                                                              | `latest` unless set by CLI config file                                         |
| `functionName`  | The name of the AWS Lambda function, which must adhere to the [AWS function naming rules](https://docs.aws.amazon.com/lambda/latest/dg/API_CreateFunction.html#SSS-CreateFunction-request-FunctionName) | `imposter-<dir name>` e.g. `imposter-example` if config dir name was `example` |
| `iamRoleName`   | The name of a custom IAM Role for the Lambda's execution role. If it doesn't exist, it will be created with this name.                                                                                  | `ImposterLambdaExecutionRole`                                                  |
| `memory`        | The amount of memory, in megabytes, for the function, such as `768`                                                                                                                                     | `768`                                                                          |
| `region`        | The AWS Region in which to deploy the function, such as `eu-west-1`                                                                                                                                     | `us-east-1`                                                                    |

#### Step 3: Deploy to Lambda

Now the remote is configured, deploy the Lambda function:

```shell
$ imposter remote deploy
```

> **Note**
> The deploy command uses the standard AWS mechanisms for locating credentials. For example, you may have set environment variables, or use the `~/.aws/` directory, or an instance role if running within EC2.
>
> If you receive a credential error, check that:
>
> - you have active AWS credentials, e.g. run `aws iam get-user`
> - you have the permissions described in the _Prerequisites_ section

Deployment may take a minute or so, depending on your connection speed, but it should look similar to:

```
deploying workspace 'example' to awslambda remote
bundling 2 files from workspace
created function: example with arn: arn:aws:lambda:us-east-1:123456789:function:example
deployed workspace 'example'

Base URL: https://url-to-invoke-lambda-function
Status: https://url-to-invoke-lambda-function/system/status
```

#### Step 4: Test your Lambda

If all has gone well, you should be able to reach your Lambda function using the status URL:

```shell
$ curl https://url-to-invoke-lambda-function/system/status

{ "status": "ok" }
```

> **Note**
> If you receive the following error:
> 
> ```
> {"Message":"Forbidden"}
> ```
> 
> ...then you may need to enable anonymous access to the Lambda function URL.
> 
> To enable anonymous access, run:
> 
> ```
> $ imposter remote config anonAccess=true
> ```
> 
> ...then redeploy with `imposter remote deploy`

You should be able to call your mock endpoints, such as:

```shell
$ curl https://url-to-invoke-lambda-function/

Hello world!
```

If you need to change a configuration option, such as memory, use the `imposter remote config` command and then run `imposter remote deploy` again.

> **Note**
> If you change the function name of your Lambda after deployment, future deployments will use the new name. This means that the old function with the previous name will still exist. Depending on your use case this may or may not be what you want to happen.

---

## What's next

- Learn how to use Imposter with the [Configuration guide](configuration.md).
