# Configuration location

Imposter can load configuration files from a directory or an S3 bucket.

This section describes how to load from these sources, depending on [how you run Imposter](getting_started.md).

> See [configuration discovery](config_discovery.md) for file naming and recursive loading rules.

## Using the CLI

### Directory

To load configuration files from a directory, pass the path to the directory in the `imposter up` command:

```bash
imposter up /path/to/config/dir
```

> **Note**
> If no directory is specified, Imposter will use the current working directory.

### S3

You can load configuration from an S3 bucket by setting the `IMPOSTER_CONFIG_DIR` environment variable to the S3 URL, such as:

```bash
imposter up -e IMPOSTER_CONFIG_DIR=s3://my-bucket/path/to/config/dir 
```

---

## Using Docker

### Directory

To load configuration files from a directory, mount the directory to `/opt/imposter/config`:

```bash
docker run -v /path/to/config/dir:/opt/imposter/config outofcoffee/imposter
```

### S3

You can load configuration from an S3 bucket by setting the `IMPOSTER_CONFIG_DIR` environment variable to the S3 URL, such as:

```bash
docker run -e IMPOSTER_CONFIG_DIR=s3://my-bucket/path/to/config/dir outofcoffee/imposter
```

---

## Using Lambda

### S3

You can load configuration from an S3 bucket by setting the `IMPOSTER_CONFIG_DIR` environment variable to the S3 URL, such as:

```bash
IMPOSTER_CONFIG_DIR=s3://my-bucket/path/to/config/dir
```

### Directory

It is not possible to load configuration files from a directory when using Lambda. Instead, bundle configuration files into a ZIP file.

To do this, use the `imposter bundle` CLI command:

```bash
imposter bundle -t awslambda /path/to/config/dir
```

This will create a ZIP file in the current working directory, which can be [deployed to Lambda](run_imposter_aws_lambda.md). The bundle contains both the Imposter engine and the configuration files.

---

## Using the JAR

### Directory

To load configuration files from a directory, pass the path to the directory in the `--config-dir` argument:

```bash
java -jar imposter.jar --configDir=/path/to/config/dir
```

### S3

You can load configuration from an S3 bucket by passing the S3 URL in the `--config-dir` argument:

```bash
java -jar imposter.jar --configDir=s3://my-bucket/path/to/config/dir
```
