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

package io.gatehill.imposter.plugin.hbase


import io.gatehill.imposter.server.BaseVerticleTest
import io.vertx.ext.unit.TestContext
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.hadoop.hbase.rest.client.Client
import org.apache.hadoop.hbase.rest.client.Cluster
import org.apache.hadoop.hbase.rest.client.RemoteHTable
import org.apache.hadoop.hbase.util.Bytes
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests for [HBasePluginImpl].
 *
 * @author Pete Cornish
 */
class HBasePluginTest : BaseVerticleTest() {
    private var client: Client? = null

    override fun getPluginClass() = HBasePluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        client = Client(Cluster().add(HOST, listenPort))
    }

    private fun expectSuccessfulRows(testContext: TestContext, table: RemoteHTable, prefix: String) {
        val scan = Scan()
        scan.filter = PrefixFilter(Bytes.toBytes(prefix))
        val scanner = table.getScanner(scan)

        var rowCount = 0
        for (result in scanner) {
            rowCount++
            testContext.assertEquals("exampleValue${rowCount}A".toString(), getStringValue(result, "abc", "exampleStringA"))
            testContext.assertEquals("exampleValue${rowCount}B".toString(), getStringValue(result, "abc", "exampleStringB"))
            testContext.assertEquals("exampleValue${rowCount}C".toString(), getStringValue(result, "abc", "exampleStringC"))
        }

        testContext.assertEquals(2, rowCount)
    }

    private fun getStringValue(result: Result, family: String, qualifier: String): String {
        return Bytes.toString(result.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier)))
    }

    @Test
    fun testFetchResults(testContext: TestContext) {
        val table = RemoteHTable(client, "exampleTable")
        expectSuccessfulRows(testContext, table, "examplePrefix")
    }

    @Test
    fun testFetchIndividualRow_Success(testContext: TestContext) {
        val table = RemoteHTable(client, "exampleTable")

        val result1 = table.get(Get(Bytes.toBytes("row1")))
        testContext.assertNotNull(result1)
        testContext.assertEquals("exampleValue1A", getStringValue(result1, "abc", "exampleStringA"))

        val result2 = table.get(Get(Bytes.toBytes("row2")))
        testContext.assertNotNull(result2)
        testContext.assertEquals("exampleValue2A", getStringValue(result2, "abc", "exampleStringA"))
    }

    @Test
    fun testFetchIndividualRow_NotFound(testContext: TestContext) {
        val table = RemoteHTable(client, "exampleTable")

        val actual = table.get(Get(Bytes.toBytes("row404")))
        testContext.assertNull(actual.row)
    }

    @Test
    fun testScriptedSuccess(testContext: TestContext) {
        val table = RemoteHTable(client, "scriptedTable")
        expectSuccessfulRows(testContext, table, "examplePrefix")
    }

    @Test
    fun testScriptedFailure(testContext: TestContext) {
        val table = RemoteHTable(client, "scriptedTable")

        val scan = Scan()
        scan.filter = PrefixFilter(Bytes.toBytes("fail"))

        try {
            table.getScanner(scan)
            testContext.fail(IOException::javaClass.name + " expected")

        } catch (e: IOException) {
            testContext.assertTrue(e.getLocalizedMessage().contains("400"))
        }
    }

    @Test
    fun testTableNotFound(testContext: TestContext) {
        val table = RemoteHTable(client, "nonExistentTable")

        val scan = Scan()
        scan.filter = PrefixFilter(Bytes.toBytes("examplePrefix"))

        try {
            table.getScanner(scan)
            testContext.fail(IOException::javaClass.name + " expected")

        } catch (e: IOException) {
            testContext.assertTrue(e.getLocalizedMessage().contains("404"))
        }
    }

    @Test
    fun testFilterMismatch(testContext: TestContext) {
        val table = RemoteHTable(client, "exampleTable")

        val scan = Scan()
        scan.filter = PrefixFilter(Bytes.toBytes("nonMatchingPrefix"))

        try {
            table.getScanner(scan)
            testContext.fail(IOException::javaClass.name + " expected")

        } catch (e: IOException) {
            testContext.assertTrue(e.getLocalizedMessage().contains("500"))
        }
    }
}
