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
        body: 'See [change log](https://github.com/outofcoffee/imposter/blob/master/CHANGELOG.md)',
    });

    const numericVersion = releaseVersion.startsWith('v') ? releaseVersion.substr(1) : releaseVersion;
    console.log(`Uploading assets...`);
    await github.rest.repos.uploadReleaseAsset({
        owner: 'outofcoffee',
        repo: 'imposter',
        release_id: release.data.id,
        name: `imposter-${numericVersion}.jar`,
        data: await fs.promises.readFile('./distro/all/build/libs/imposter-all.jar'),
    });

    console.log(`Assets uploaded to release: ${releaseVersion}`);
};
