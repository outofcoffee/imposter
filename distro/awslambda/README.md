# AWS Lambda distribution

## Set up

- Create an S3 bucket
- Upload the files from `deploy/example/config` to the bucket, under a directory (e.g. `config`)
- Set the S3 path to the config dir in `deploy/serverless.yml` as the `IMPOSTER_S3_CONFIG_URL` environment variable

## One shot build/deploy

Using Node.js:

    cd deploy
    npm install
    npm run deploy

## Step by step

### Build

    ./gradlew shadowJar

Builds JAR to `./build/libs`

### Deploy

     npx serverless deploy

Or:

    npm install
    npm run deploy
