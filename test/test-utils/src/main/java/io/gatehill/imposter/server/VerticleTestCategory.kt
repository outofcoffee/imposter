package io.gatehill.imposter.server

import org.junit.jupiter.api.Tag

/**
 * Marker annotation for tests that extend BaseVerticleTest.
 * Used to categorise and run these tests specifically.
 *
 * @author Pete Cornish
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("verticle")
annotation class VerticleTest
