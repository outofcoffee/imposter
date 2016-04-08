package com.gatehill.imposter.plugin.sfdc;

import com.force.api.ApiConfig;
import com.force.api.ForceApi;
import com.force.api.QueryResult;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.sfdc.support.Account;
import com.gatehill.imposter.server.BaseVerticleTest;
import com.gatehill.imposter.util.CryptoUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import static com.gatehill.imposter.server.ImposterVerticle.CONFIG_PREFIX;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginImplTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return SfdcPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        // enable TLS before deployment
        System.setProperty(CONFIG_PREFIX + "tls", "true");

        // deploy
        super.setUp(testContext);

        // set up trust store for TLS
        System.setProperty("javax.net.ssl.trustStore", CryptoUtil.getKeystore(SfdcPluginImplTest.class).toString());
        System.setProperty("javax.net.ssl.trustStorePassword", CryptoUtil.KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        // for localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, sslSession) -> hostname.equals("localhost"));
    }

    private ForceApi buildForceApi() {
        return new ForceApi(new ApiConfig()
                .setForceURL("https://" + HOST + ":" + getListenPort() + "/?")
                .setUsername("user@example.com")
                .setPassword("password")
                .setClientId("longclientidalphanumstring")
                .setClientSecret("notsolongnumeric"));
    }

    @Test
    public void testQueryRecordsSuccess(TestContext testContext) {
        // Something like 'SELECT Name, Id from Account LIMIT 100' becomes:
        // http://localhost:8443/services/data/v20.0/query?q=SELECT%20Name,%20Id%20from%20Account%20LIMIT%20100

        final ForceApi api = buildForceApi();

        final QueryResult<Account> actual = api.query("SELECT Name, Id from Account LIMIT 100", Account.class);
        testContext.assertNotNull(actual);
        testContext.assertTrue(actual.isDone());

        // check records
        testContext.assertEquals(2, actual.getRecords().size());

        testContext.assertTrue(actual.getRecords().stream()
                .anyMatch(account -> "0015000000VALDtAAP".equals(account.getId())));

        testContext.assertTrue(actual.getRecords().stream()
                .anyMatch(account -> "0015000000XALDuAAZ".equals(account.getId())));
    }

    @Test
    public void testGetRecordByIdSuccess(TestContext testContext) {
        // Query for specific object with ID, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/0015000000VALDtAAP/

        final ForceApi api = buildForceApi();

        final Account actual = api.getSObject("Account", "0015000000VALDtAAP").as(Account.class);
        testContext.assertNotNull(actual);
        testContext.assertEquals("0015000000VALDtAAP", actual.getId());
        testContext.assertEquals("GenePoint", actual.getName());
    }
}
