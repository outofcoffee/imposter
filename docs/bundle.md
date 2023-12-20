# Bundle configuration

Bundling is the inclusion of mock configuration alongside the Imposter binaries in a deployment package, such as a container image or Lambda ZIP file.

This keeps all elements of the mock in a single package, simplifying deployment and distribution of your mocks.

### Advantages

- Engine and configuration can be deployed in a single step
- Engine and configuration don't require an external mechanism to keep them in sync
- Engine does not have to be restarted by an external mechanism to pick up new configuration

### Disadvantages

- Engine is also deployed when updating configuration
- Combined deployment package is larger than deploying configuration alone

## How to bundle configuration

There are different ways to bundle configuration, depending on the deployment target.

### AWS Lambda bundles

When [deploying to AWS Lambda](./run_imposter_aws_lambda.md), you can bundle the configuration files into the Lambda ZIP file.

#### Creating a bundle for Lambda (automated)

You can create a bundle using [the CLI](./run_imposter_cli.md) using the `imposter bundle` command.

```shell
$ imposter bundle -t awslambda -o bundle.zip

creating awslambda bundle /users/person/mock using version 3.32.0
downloading https://github.com/outofcoffee/imposter/releases/download/v3.32.0/imposter-awslambda.zip
bundling 3 files from workspace
created deployment package
created awslambda bundle: /users/person/mock/bundle.zip
```

The bundle file (`bundle.zip` in this example), can be [deployed to AWS Lambda](./run_imposter_aws_lambda.md) as normal.

#### Creating a bundle for Lambda (manual)

If you do not want to use the CLI, you can create a bundle using standard `zip` tools.

Download the latest `imposter-awslambda.zip` distribution from the [Releases page](https://github.com/outofcoffee/imposter/releases).

Let's assume your configuration sits in a directory called `config`.

```shell
$ ls -l
drwxr-xr-x  5 person  wheel  160B 16 Nov 13:12 config
-rw-r--r--  1 person  staff  20M  16 Nov 13:12 imposter-awslambda.zip
```

Add the config to the ZIP file:

```shell
$ zip -ur imposter-awslambda.zip config

  adding: config/ (stored 0%)
  adding: config/mock-config.yaml (deflated 19%)
  adding: config/response.json (stored 0%)
  adding: config/mock.txt (deflated 22%)
```

The `imposter-bundle.zip` file can be [deployed to AWS Lambda](./run_imposter_aws_lambda.md) as normal.

---

### Docker bundles

When [deploying using containers](./run_imposter_docker.md), you can add the configuration files into the container image itself.

#### Creating a container image bundle (automated)

You can create a bundle using [the CLI](./run_imposter_cli.md) using the `imposter bundle` command.

Let's assume your configuration sits in a file called `some-config.yaml`:

```shell
$ ls
some-config.yaml
```

Bundle it:

```shell
$ imposter bundle -t docker -o example/mock:v1
DEBU[0000] creating docker bundle /users/person/mock using version 3.33.3
DEBU[0000] engine image '3.33.3' already present
...
Successfully built c791e6281b26
Successfully tagged example/mock:v1
INFO[0000] build process completed
INFO[0000] created docker bundle: example/mock:v1
```

This command created a container image called `example/mock:v1` containing the configuration file and the Imposter mock engine.

This is a standard Docker container image, so you can push it to a registry and run it anywhere Docker runs.

> **Note**
> The container image in this example is tagged as `example/mock:v1` but you can specify any valid container name as the `-o NAME` option. 

Run the container:

```shell
$ docker run -it -p 8080:8080 example/mock:v1
...
Loading configuration file: ConfigReference(file=/opt/imposter/config/some-config.yaml, configRoot=/opt/imposter/config)
Mock engine up and running on http://localhost:8080
```

The mock server is running at [http://localhost:8080](http://localhost:8080).

#### Creating a container image bundle (manual)

Let's assume your configuration sits in a directory called `config`.

Here is an example Dockerfile:

```dockerfile
FROM outofcoffee/imposter

# your custom config
COPY config /opt/imposter/config
```

Build it:

```shell
$ docker build --tag example/mocks .
```

The container image (`example/mocks` in this example), can be [run with Docker](./run_imposter_docker.md) as normal.

```shell
$ docker run --rm -it -p 8080:8080 example/mocks
```

This is a standard Docker container image, so you can push it to a registry and run it anywhere Docker runs.

> See [the Docker example project](https://github.com/outofcoffee/imposter/tree/main/examples/docker) for a working example.

