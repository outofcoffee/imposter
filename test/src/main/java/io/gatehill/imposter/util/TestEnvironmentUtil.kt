/*
 * Copyright (c) 2016-2021.
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
package io.gatehill.imposter.util

import org.hamcrest.CoreMatchers
import org.junit.Assume
import java.io.File

/**
 * @author Pete Cornish
 */
object TestEnvironmentUtil {
    private val NULL_FILE = File(
        if (System.getProperty("os.name").startsWith("Windows")) "NUL" else "/dev/null"
    )

    /**
     * Skips a JUnit test if Docker is not accessible.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun assumeDockerAccessible() {
        Assume.assumeThat("Docker is running", testDockerRunning(), CoreMatchers.`is`(0))
    }

    /**
     * Tests if the Docker process can be started successfully.
     *
     *
     * Typically used with a [org.junit.Assume.assumeThat] statement in a JUnit test,
     * for example:
     *
     * <pre>
     * assumeThat("Docker is running", TestEnvironmentUtil.testDockerRunning(), is(0));
    </pre> *
     *
     * @return the exit code of running the 'docker ps' command
     */
    private fun testDockerRunning(): Int {
        return try {
            ProcessBuilder("docker", "ps")
                .redirectErrorStream(true)
                .redirectOutput(NULL_FILE)
                .start().waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            255
        }
    }
}