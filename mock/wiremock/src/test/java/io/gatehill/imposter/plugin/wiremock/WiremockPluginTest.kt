package io.gatehill.imposter.plugin.wiremock

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.EnvVars
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
    @Test
    fun `can convert unwrapped wiremock mappings`() {
        val configDir = convert("/wiremock-nowrap")

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(2, files?.size)

        assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        assertThat(files, CoreMatchers.hasItem("files"))
    }

    @Test
    fun `can convert simple wiremock mappings to single config file`() {
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
    fun `can convert simple wiremock mappings to separate config files`() {
        val configDir = convert("/wiremock-simple", 2, "IMPOSTER_WIREMOCK_SEPARATE_CONFIG" to "true")

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(3, files?.size)

        assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        assertThat(files, CoreMatchers.hasItem("wiremock-1-config.json"))
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

    private fun convert(mappingsPath: String, expectedConfigFiles: Int = 1, vararg env: Pair<String, String>): File {
        val mappingsDir = File(WiremockPluginTest::class.java.getResource(mappingsPath)!!.toURI())

        if (env.isNotEmpty()) {
            EnvVars.populate(*env)
        }

        val wiremock = WiremockPluginImpl(mock(), ImposterConfig(), mock(), mock(), mock())
        val configFiles = wiremock.convert(File(mappingsDir, "imposter-config.yaml"))
        assertThat(configFiles, hasSize(expectedConfigFiles))

        val configDir = configFiles.first().parentFile

        Assert.assertTrue("Config dir should exist", configDir.exists())
        assertThat(
            "Config dir should differ from source dir",
            configDir,
            not(equalTo(mappingsDir))
        )
        return configDir
    }
}
