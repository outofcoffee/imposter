package org.testcontainers.containers

import org.testcontainers.images.RemoteDockerImage
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.Future

/**
 * @author Pete Cornish
 */
class GenericKontainer : GenericContainer<GenericKontainer> {
    constructor(imageName: String) : super(imageName)
    constructor(image: Future<String>) : super(image)
    constructor(dockerImageName: DockerImageName) : super(dockerImageName)
    constructor(image: RemoteDockerImage) : super(image)
}
