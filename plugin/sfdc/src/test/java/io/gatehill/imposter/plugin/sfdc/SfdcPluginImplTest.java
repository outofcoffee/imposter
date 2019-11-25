package io.gatehill.imposter.plugin.sfdc;

import com.force.api.ApiConfig;
import com.force.api.ForceApi;
import com.force.api.QueryResult;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.sfdc.support.Account;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.CryptoUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD;
import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH;
import static io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX;

/**
 * Tests for {@link SfdcPluginImpl}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SfdcPluginImplTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return SfdcPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);

        // set up trust store for TLS
        System.setProperty("javax.net.ssl.trustStore", CryptoUtil.getDefaultKeystore(SfdcPluginImplTest.class).toString());
        System.setProperty("javax.net.ssl.trustStorePassword", CryptoUtil.DEFAULT_KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        // for localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, sslSession) -> hostname.equals("localhost"));
    }

    @Override
    protected void configure(ImposterConfig imposterConfig) throws Exception {
        super.configure(imposterConfig);

        // enable TLS
        imposterConfig.setTlsEnabled(true);
        imposterConfig.setKeystorePath(CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH);
        imposterConfig.setKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
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
        // Something like 'SELECT Name, Id from Account LIMIT 100' becomes GET to:
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
        // GET Query for specific object with ID, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/0015000000VALDtAAP/

        final ForceApi api = buildForceApi();

        final Account actual = api.getSObject("Account", "0015000000VALDtAAP").as(Account.class);
        testContext.assertNotNull(actual);
        testContext.assertEquals("0015000000VALDtAAP", actual.getId());
        testContext.assertEquals("GenePoint", actual.getName());
    }

    @Test(expected = RuntimeException.class)
    public void testRecordNotFound() {
        final ForceApi api = buildForceApi();

        api.getSObject("Account", "nonExistentId").as(Account.class);
    }

    @Test
    public void testCreateRecord(TestContext testContext) {
        // POST to create object, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/

        final Account account = new Account();
        account.setName("NewAccount");

        final ForceApi api = buildForceApi();

        final String actual = api.createSObject("Account", account);
        testContext.assertNotNull(actual);
    }

    @Test
    public void testUpdateRecord() {
        final ForceApi api = buildForceApi();

        // get current SObject first
        final Account account = api.getSObject("Account", "0015000000XALDuAAZ").as(Account.class);

        // PATCH to create object, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/0015000000VALDtAAP

        account.setName("UpdatedName");
        api.updateSObject("Account", account.getId(), account);
    }
}
