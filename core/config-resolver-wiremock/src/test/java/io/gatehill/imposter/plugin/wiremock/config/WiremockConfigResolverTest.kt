package io.gatehill.imposter.plugin.wiremock.config

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test
import java.io.File

/**
 * Verifies converting wiremock mappings to Imposter config.
 *
 * @author Pete Cornish
 */
class WiremockConfigResolverTest {
    private val configResolver = WiremockConfigResolver()

    @Test
    fun `can handle dir containing wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-simple")!!.toURI())
        Assert.assertTrue("Should handle wiremock mappings dir", configResolver.handles(mappingsDir.absolutePath))

        val configDir = File(WiremockConfigResolverTest::class.java.getResource("/config")!!.toURI())
        Assert.assertFalse("Should not handle imposter config dir", configResolver.handles(configDir.absolutePath))
    }

    @Test
    fun `can convert simple wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-simple")!!.toURI())

        val configDir = configResolver.resolve(mappingsDir.absolutePath)
        Assert.assertTrue("Config dir should exist", configDir.exists())
        MatcherAssert.assertThat(
            "Config dir should differ from source dir",
            configDir,
            CoreMatchers.not(CoreMatchers.equalTo(mappingsDir))
        )

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(2, files?.size)

        MatcherAssert.assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        MatcherAssert.assertThat(files, CoreMatchers.hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assert.assertEquals(1, responseFiles?.size)
        MatcherAssert.assertThat(responseFiles, CoreMatchers.hasItem("response.json"))
    }

    @Test
    fun `can convert templated wiremock mappings`() {
        val mappingsDir = File(WiremockConfigResolverTest::class.java.getResource("/wiremock-templated")!!.toURI())

        val configDir = configResolver.resolve(mappingsDir.absolutePath)
        Assert.assertTrue("Config dir should exist", configDir.exists())
        MatcherAssert.assertThat(
            "Config dir should differ from source dir",
            configDir,
            CoreMatchers.not(CoreMatchers.equalTo(mappingsDir))
        )

        val files = configDir.listFiles()?.map { it.name }
        Assert.assertEquals(2, files?.size)

        MatcherAssert.assertThat(files, CoreMatchers.hasItem("wiremock-0-config.json"))
        MatcherAssert.assertThat(files, CoreMatchers.hasItem("files"))

        val responseFileDir = File(configDir, "files")
        val responseFiles = responseFileDir.listFiles()?.map { it.name }
        Assert.assertEquals(1, responseFiles?.size)
        MatcherAssert.assertThat(responseFiles, CoreMatchers.hasItem("response.xml"))

        val responseFile = File(responseFileDir, "response.xml").readText()
        MatcherAssert.assertThat(responseFile, CoreMatchers.not(CoreMatchers.containsString("{{")))
        MatcherAssert.assertThat(
            responseFile,
            CoreMatchers.containsString("\${context.request.body://getPetByIdRequest/id}")
        )
        MatcherAssert.assertThat(
            responseFile,
            CoreMatchers.containsString("\${random.alphabetic(length=5,uppercase=true)}")
        )
    }
}