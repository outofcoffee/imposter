package com.gatehill.imposter;

import com.gatehill.imposter.plugins.hbase.HBasePluginImpl;
import com.gatehill.imposter.server.ImposterVerticle;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.rest.client.Client;
import org.apache.hadoop.hbase.rest.client.Cluster;
import org.apache.hadoop.hbase.rest.client.RemoteHTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Paths;

import static com.gatehill.imposter.server.ImposterVerticle.CONFIG_PREFIX;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class HBasePluginTest {
    private static final int LISTEN_PORT = 8443;
    private static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();
    private Client client;

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        System.setProperty(CONFIG_PREFIX + "configDir", Paths.get(HBasePluginTest.class.getResource("/config").toURI()).toString());
        System.setProperty(CONFIG_PREFIX + "pluginClass", HBasePluginImpl.class.getCanonicalName());
        System.setProperty(CONFIG_PREFIX + "host", HOST);
        System.setProperty(CONFIG_PREFIX + "listenPort", String.valueOf(LISTEN_PORT));

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });

        client = new Client(new Cluster().add(HOST, LISTEN_PORT));
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
            testContext.assertEquals("exampleValue", Bytes.toString(result.getValue(Bytes.toBytes("abc"), Bytes.toBytes("exampleCell"))));
        }

        testContext.assertEquals(1, rowCount);
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
