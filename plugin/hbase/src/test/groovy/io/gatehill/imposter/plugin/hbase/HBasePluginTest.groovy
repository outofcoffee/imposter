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


import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.server.BaseVerticleTest
import io.vertx.ext.unit.TestContext
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.ResultScanner
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.hadoop.hbase.rest.client.Client
import org.apache.hadoop.hbase.rest.client.Cluster
import org.apache.hadoop.hbase.rest.client.RemoteHTable
import org.apache.hadoop.hbase.util.Bytes
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link HBasePluginImpl}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class HBasePluginTest extends BaseVerticleTest {
    private Client client

    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return HBasePluginImpl.class
    }

    @Before
    void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext)

        client = new Client(new Cluster().add(HOST, getListenPort()))
    }

    private static void expectSuccessfulRows(TestContext testContext, RemoteHTable table, String prefix) throws IOException {
        final Scan scan = new Scan()
        scan.setFilter(new PrefixFilter(Bytes.toBytes(prefix)))
        final ResultScanner scanner = table.getScanner(scan)

        int rowCount = 0
        for (Result result : scanner) {
            rowCount++
            testContext.assertEquals("exampleValue${rowCount}A".toString(), getStringValue(result, "abc", "exampleStringA"))
            testContext.assertEquals("exampleValue${rowCount}B".toString(), getStringValue(result, "abc", "exampleStringB"))
            testContext.assertEquals("exampleValue${rowCount}C".toString(), getStringValue(result, "abc", "exampleStringC"))
        }

        testContext.assertEquals(2, rowCount)
    }

    private static String getStringValue(Result result, String family, String qualifier) {
        return Bytes.toString(result.getValue(Bytes.toBytes(family), Bytes.toBytes(qualifier)))
    }

    @Test
    void testFetchResults(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable")
        expectSuccessfulRows(testContext, table, "examplePrefix")
    }

    @Test
    void testFetchIndividualRow_Success(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable")

        final Result result1 = table.get(new Get(Bytes.toBytes("row1")))
        testContext.assertNotNull(result1)
        testContext.assertEquals("exampleValue1A", getStringValue(result1, "abc", "exampleStringA"))

        final Result result2 = table.get(new Get(Bytes.toBytes("row2")))
        testContext.assertNotNull(result2)
        testContext.assertEquals("exampleValue2A", getStringValue(result2, "abc", "exampleStringA"))
    }

    @Test
    void testFetchIndividualRow_NotFound(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable")

        def actual = table.get(new Get(Bytes.toBytes("row404")))
        testContext.assertNull(actual.row)
    }

    @Test
    void testScriptedSuccess(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "scriptedTable")
        expectSuccessfulRows(testContext, table, "examplePrefix")
    }

    @Test
    void testScriptedFailure(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "scriptedTable")

        final Scan scan = new Scan()
        scan.setFilter(new PrefixFilter(Bytes.toBytes("fail")))

        try {
            table.getScanner(scan)
            testContext.fail(IOException.class.simpleName + " expected")

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("400"))
        }
    }

    @Test
    void testTableNotFound(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "nonExistentTable")

        final Scan scan = new Scan()
        scan.setFilter(new PrefixFilter(Bytes.toBytes("examplePrefix")))

        try {
            table.getScanner(scan)
            testContext.fail(IOException.class.simpleName + " expected")

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("404"))
        }
    }

    @Test
    void testFilterMismatch(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable")

        final Scan scan = new Scan()
        scan.setFilter(new PrefixFilter(Bytes.toBytes("nonMatchingPrefix")))

        try {
            table.getScanner(scan)
            testContext.fail(IOException.class.simpleName + " expected")

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("500"))
        }
    }
}
