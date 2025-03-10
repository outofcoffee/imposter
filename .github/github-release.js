/*
 * Copyright (c) 2023.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    await publishRelease(github, releaseId);
};

/**
 * @param releaseVersion
 * @param github
 * @returns {Promise<string|null>} the release ID, or `null`
 */
async function getExistingRelease(releaseVersion, github) {
    console.log(`Checking for release: ${releaseVersion}`);
    const releases = await github.rest.repos.listReleases({
        owner: 'imposter-project',
        repo: 'imposter-jvm-engine',
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

    let body = '';
    try {
        body = await fs.promises.readFile('./build/CHANGES.md', 'utf8');
    } catch (e) {
        console.warn(`Failed to read CHANGES.md: ${e}`);
    }
    body += '\n\nSee [change log](https://github.com/imposter-project/imposter-jvm-engine/blob/main/CHANGELOG.md).'

    const release = await github.rest.repos.createRelease({
        owner: 'imposter-project',
        repo: 'imposter-jvm-engine',
        tag_name: releaseVersion,
        body,
        draft: true,
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
        owner: 'imposter-project',
        repo: 'imposter-jvm-engine',
        release_id: releaseId,
        name: assetFileName,
        data: await fs.promises.readFile(localFilePath),
    });
}

async function publishRelease(github, releaseId) {
    console.log(`Publishing release ${releaseId}...`);
    await github.rest.repos.updateRelease({
        owner: 'imposter-project',
        repo: 'imposter-jvm-engine',
        release_id: releaseId,
        draft: false,
    });
}
