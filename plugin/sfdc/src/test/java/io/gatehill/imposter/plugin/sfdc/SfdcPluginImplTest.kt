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
package io.gatehill.imposter.plugin.sfdc

import com.force.api.ApiConfig
import com.force.api.ForceApi
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.plugin.sfdc.support.Account
import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH
import io.gatehill.imposter.util.CryptoUtil.getDefaultKeystore
import io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX
import io.vertx.ext.unit.TestContext
import org.junit.Before
import org.junit.Test
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

/**
 * Tests for [SfdcPluginImpl].
 *
 * @author Pete Cornish
 */
class SfdcPluginImplTest : BaseVerticleTest() {
    override val pluginClass = SfdcPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)

        // set up trust store for TLS
        System.setProperty("javax.net.ssl.trustStore", getDefaultKeystore(SfdcPluginImplTest::class.java).toString())
        System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEYSTORE_PASSWORD)
        System.setProperty("javax.net.ssl.trustStoreType", "JKS")

        // for localhost testing only
        HttpsURLConnection.setDefaultHostnameVerifier { hostname: String, _: SSLSession? -> hostname == "localhost" }
    }

    @Throws(Exception::class)
    override fun configure(imposterConfig: ImposterConfig) {
        super.configure(imposterConfig)

        // enable TLS
        imposterConfig.isTlsEnabled = true
        imposterConfig.keystorePath = CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH
        imposterConfig.keystorePassword = DEFAULT_KEYSTORE_PASSWORD
    }

    private fun buildForceApi(): ForceApi {
        return ForceApi(
            ApiConfig()
                .setForceURL("https://$host:$listenPort/?")
                .setUsername("user@example.com")
                .setPassword("password")
                .setClientId("longclientidalphanumstring")
                .setClientSecret("notsolongnumeric")
        )
    }

    @Test
    fun testQueryRecordsSuccess(testContext: TestContext) {
        // Something like 'SELECT Name, Id from Account LIMIT 100' becomes GET to:
        // http://localhost:8443/services/data/v20.0/query?q=SELECT%20Name,%20Id%20from%20Account%20LIMIT%20100
        val api = buildForceApi()
        val actual = api.query("SELECT Name, Id from Account LIMIT 100", Account::class.java)
        testContext.assertNotNull(actual)
        testContext.assertTrue(actual.isDone)

        // check records
        testContext.assertEquals(2, actual.records.size)
        testContext.assertTrue(actual.records.any { account: Account -> "0015000000VALDtAAP" == account.id })
        testContext.assertTrue(actual.records.any { account: Account -> "0015000000XALDuAAZ" == account.id })
    }

    @Test
    fun testGetRecordByIdSuccess(testContext: TestContext) {
        // GET Query for specific object with ID, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/0015000000VALDtAAP/
        val api = buildForceApi()
        val actual = api.getSObject("Account", "0015000000VALDtAAP").`as`(Account::class.java)
        testContext.assertNotNull(actual)
        testContext.assertEquals("0015000000VALDtAAP", actual.id)
        testContext.assertEquals("GenePoint", actual.name)
    }

    @Test(expected = RuntimeException::class)
    fun testRecordNotFound() {
        val api = buildForceApi()
        api.getSObject("Account", "nonExistentId").`as`(Account::class.java)
    }

    @Test
    fun testCreateRecord(testContext: TestContext) {
        // POST to create object, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/
        val account = Account()
        account.name = "NewAccount"
        val api = buildForceApi()
        val actual = api.createSObject("Account", account)
        testContext.assertNotNull(actual)
    }

    @Test
    fun testUpdateRecord() {
        val api = buildForceApi()

        // get current SObject first
        val account = api.getSObject("Account", "0015000000XALDuAAZ").`as`(Account::class.java)

        // PATCH to create object, like:
        // http://localhost:8443/services/data/v20.0/sobjects/Account/0015000000VALDtAAP
        account.name = "UpdatedName"
        api.updateSObject("Account", account.id, account)
    }
}