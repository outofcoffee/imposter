const fs = require('fs');

const MainDistroAlias = 'main';

/**
 * @param github github client
 * @param context job context
 * @param assetPaths {Array<string>|string}
 * @returns {Promise<void>}
 */
module.exports = async ({github, context}, assetPaths) => {
    if (!context.ref.match(/refs\/tags\/.+/)) {
        console.warn(`Unsupported ref: ${context.ref}`);
        return;
    }
    const releaseVersion = context.ref.split('/')[2];
    if (!releaseVersion) {
        console.warn('No release version - aborting');
        return;
    }
    if (!assetPaths) {
        console.warn('No asset paths - aborting');
        return;
    }

    let releaseId = await getExistingRelease(releaseVersion, github);
    if (!releaseId) {
        releaseId = await createRelease(releaseVersion, github);
    }

    if (!Array.isArray(assetPaths)) {
        assetPaths = [assetPaths];
    }
    for (const assetPath of assetPaths) {
        if (assetPath === MainDistroAlias) {
            await releaseMainDistro(github, releaseId, releaseVersion);
        } else {
            await releaseJar(github, releaseId, assetPath)
        }
    }
    console.log(`Assets uploaded to release: ${releaseVersion}`);
};

/**
 * @param releaseVersion
 * @param github
 * @returns {Promise<string|null>} the release ID, or `null`
 */
async function getExistingRelease(releaseVersion, github) {
    console.log(`Checking for release: ${releaseVersion}`);
    const releases = await github.rest.repos.listReleases({
        owner: 'outofcoffee',
        repo: 'imposter',
    });
    let releaseId;
    for (const release of releases.data) {
        if (release.tag_name === releaseVersion) {
            releaseId = release.id;
            console.log(`Found existing release with ID: ${releaseId}`);
            break;
        }
    }
    return releaseId;
}

/**
 * @param releaseVersion
 * @param github
 * @returns {Promise<string>} the release ID
 */
async function createRelease(releaseVersion, github) {
    console.log(`Creating release: ${releaseVersion}`);
    const release = await github.rest.repos.createRelease({
        owner: 'outofcoffee',
        repo: 'imposter',
        tag_name: releaseVersion,
        body: 'See [change log](https://github.com/outofcoffee/imposter/blob/main/CHANGELOG.md).',
    });
    return release.data.id;
}

async function releaseMainDistro(github, releaseId, releaseVersion) {
    const localFilePath = './distro/core/build/libs/imposter-core.jar';
    await uploadAsset(github, releaseId, 'imposter.jar', localFilePath);

    // upload with version suffix, for compatibility with cli < 0.7.0
    const numericVersion = releaseVersion.startsWith('v') ? releaseVersion.substr(1) : releaseVersion;
    await uploadAsset(github, releaseId, `imposter-${numericVersion}.jar`, localFilePath);
}

async function releaseJar(github, releaseId, localFilePath) {
    const assetFileName = localFilePath.substr(localFilePath.lastIndexOf('/') + 1);
    await uploadAsset(github, releaseId, assetFileName, localFilePath);
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
