package io.gatehill.imposter.plugin.wiremock

import io.gatehill.imposter.ImposterConfig
import io.vertx.core.Vertx
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

/**
 * Verifies converting wiremock mappings to Imposter config.
 *
 * @author Pete Cornish
 */
class WiremockPluginTest {
    private val configResolver = WiremockPluginImpl(
        Vertx.vertx(),
        ImposterConfig(),
        mock(),
        mock(),
        mock(),
    )

    @Test
    fun `can convert simple wiremock mappings`() {
        val configDir = convert("/wiremock-simple")

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(2, files?.size)

        assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        assertThat(files, CoreMatchers.hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assert.assertEquals(1, responseFiles?.size)
        assertThat(responseFiles, CoreMatchers.hasItem("response.json"))
    }

    @Test
    fun `can convert templated wiremock mappings`() {
        val configDir = convert("/wiremock-templated")

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(2, files?.size)

        assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        assertThat(files, CoreMatchers.hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assert.assertEquals(1, responseFiles?.size)
        assertThat(responseFiles, CoreMatchers.hasItem("response.xml"))

        val responseFile = File(responseFileDir, "response.xml").readText()
        assertThat(responseFile, not(CoreMatchers.containsString("{{")))
        assertThat(
            responseFile,
            CoreMatchers.containsString("\${context.request.body://getPetByIdRequest/id}")
        )
        assertThat(
            responseFile,
            CoreMatchers.containsString("\${random.alphabetic(length=5,uppercase=true)}")
        )
    }

    private fun convert(mappingsPath: String): File {
        val mappingsDir = File(WiremockPluginTest::class.java.getResource(mappingsPath)!!.toURI())
        val configFiles = configResolver.convert(File(mappingsDir, "imposter-config.yaml"))

        val configDirs = configFiles.map { it.parentFile }
        assertThat(configDirs, hasSize(1))
        val configDir = configDirs.first()

        Assert.assertTrue("Config dir should exist", configDir.exists())
        assertThat(
            "Config dir should differ from source dir",
            configDir,
            not(equalTo(mappingsDir))
        )
        return configDir
    }
}
