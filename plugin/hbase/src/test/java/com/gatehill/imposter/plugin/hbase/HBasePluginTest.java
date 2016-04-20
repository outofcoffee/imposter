package com.gatehill.imposter.plugin.hbase;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.server.BaseVerticleTest;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.rest.client.Client;
import org.apache.hadoop.hbase.rest.client.Cluster;
import org.apache.hadoop.hbase.rest.client.RemoteHTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class HBasePluginTest extends BaseVerticleTest {
    private Client client;

    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return HBasePluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);

        client = new Client(new Cluster().add(HOST, getListenPort()));
    }

    @Test
    public void testFetchResults(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable");

        final Scan scan = new Scan();
        scan.setFilter(new PrefixFilter(Bytes.toBytes("examplePrefix")));
        final ResultScanner scanner = table.getScanner(scan);

        int rowCount = 0;
        for (Result result : scanner) {
            rowCount++;
            testContext.assertEquals("exampleValueA", Bytes.toString(result.getValue(Bytes.toBytes("abc"), Bytes.toBytes("exampleStringA"))));
            testContext.assertEquals("exampleValueB", Bytes.toString(result.getValue(Bytes.toBytes("abc"), Bytes.toBytes("exampleStringB"))));
            testContext.assertEquals("exampleValueC", Bytes.toString(result.getValue(Bytes.toBytes("abc"), Bytes.toBytes("exampleStringC"))));
        }

        testContext.assertEquals(1, rowCount);
    }

    @Test
    public void testFetchSingleRow(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable");

        final Result result = table.get(new Get(Bytes.toBytes("record1")));
        testContext.assertEquals("exampleValueA", Bytes.toString(result.getValue(Bytes.toBytes("abc"), Bytes.toBytes("exampleStringA"))));
    }

    @Test
    public void testScriptedFailure(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "scriptedTable");

        final Scan scan = new Scan();
        scan.setFilter(new PrefixFilter(Bytes.toBytes("fail")));

        try {
            table.getScanner(scan);
            testContext.fail(IOException.class + " expected");

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("400"));
        }
    }

    @Test
    public void testTableNotFound(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "nonExistentTable");

        final Scan scan = new Scan();
        scan.setFilter(new PrefixFilter(Bytes.toBytes("examplePrefix")));

        try {
            table.getScanner(scan);
            testContext.fail(IOException.class + " expected");

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("404"));
        }
    }

    @Test
    public void testFilterMismatch(TestContext testContext) throws Exception {
        final RemoteHTable table = new RemoteHTable(client, "exampleTable");

        final Scan scan = new Scan();
        scan.setFilter(new PrefixFilter(Bytes.toBytes("nonMatchingPrefix")));

        try {
            table.getScanner(scan);
            testContext.fail(IOException.class + " expected");

        } catch (IOException e) {
            testContext.assertTrue(e.getLocalizedMessage().contains("500"));
        }
    }
}
