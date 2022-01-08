const fs = require('fs');

module.exports = async ({github, context}) => {
    if (!context.ref.match(/refs\/tags\/.+/)) {
        console.warn(`Unsupported ref: ${context.ref}`);
        return;
    }
    const releaseVersion = context.ref.split('/')[2];
    if (!releaseVersion) {
        console.warn('No release version - aborting');
        return;
    }

    console.log(`Creating release: ${releaseVersion}`);
    const release = await github.rest.repos.createRelease({
        owner: 'outofcoffee',
        repo: 'imposter',
        tag_name: releaseVersion,
        body: 'See [change log](https://github.com/outofcoffee/imposter/blob/main/CHANGELOG.md).',
    });

    await releaseMainDistro(github, release, releaseVersion);
    await releaseLambdaDistro(github, release);
    await releasePlugins(github, release);

    console.log(`Assets uploaded to release: ${releaseVersion}`);
};

async function releaseMainDistro(github, release, releaseVersion) {
    const localFilePath = './distro/core/build/libs/imposter-core.jar';
    await uploadAsset(github, release.data.id, 'imposter.jar', localFilePath, release.data.id);

    // upload with version suffix, for compatibility with cli < 0.7.0
    const numericVersion = releaseVersion.startsWith('v') ? releaseVersion.substr(1) : releaseVersion;
    await uploadAsset(github, release.data.id, `imposter-${numericVersion}.jar`, localFilePath, release.data.id);
}

async function releaseLambdaDistro(github, release) {
    await releaseJar(github, release, './distro/awslambda/build/libs/imposter-awslambda.jar');
}

async function releasePlugins(github, release) {
    await releaseJar(github, release, './store/dynamodb/build/libs/imposter-plugin-store-dynamodb.jar');
    await releaseJar(github, release, './store/redis/build/libs/imposter-plugin-store-redis.jar');
}

async function releaseJar(github, release, localFilePath) {
    const assetFileName = localFilePath.substr(localFilePath.lastIndexOf('/') + 1);
    await uploadAsset(github, release.data.id, assetFileName, localFilePath, release.data.id);
}

async function uploadAsset(github, releaseId, assetFileName, localFilePath) {
    console.log(`Uploading ${localFilePath} as release asset ${assetFileName}...`);
    await github.rest.repos.uploadReleaseAsset({
        owner: 'outofcoffee',
        repo: 'imposter',
        release_id: releaseId,
        name: assetFileName,
        data: await fs.promises.readFile(localFilePath),
    });
}
