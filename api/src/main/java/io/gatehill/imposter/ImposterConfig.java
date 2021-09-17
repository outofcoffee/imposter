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

package io.gatehill.imposter;

import io.gatehill.imposter.server.RequestHandlingMode;

import java.util.Map;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterConfig {
    private String host;
    private int listenPort;
    private String[] configDirs;
    private String serverUrl;
    private boolean tlsEnabled;
    private String keystorePath;
    private String keystorePassword;
    private String[] plugins;
    private Map<String, String> pluginArgs;
    private String serverFactory;
    private RequestHandlingMode requestHandlingMode = RequestHandlingMode.ASYNC;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String[] getConfigDirs() {
        return configDirs;
    }

    public void setConfigDirs(String[] configDirs) {
        this.configDirs = configDirs;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setPlugins(String[] plugins) {
        this.plugins = plugins;
    }

    public String[] getPlugins() {
        return plugins;
    }

    public void setPluginArgs(Map<String, String> pluginArgs) {
        this.pluginArgs = pluginArgs;
    }

    public Map<String, String> getPluginArgs() {
        return pluginArgs;
    }

    public String getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(String serverFactory) {
        this.serverFactory = serverFactory;
    }

    public RequestHandlingMode getRequestHandlingMode() {
        return requestHandlingMode;
    }

    public void setRequestHandlingMode(RequestHandlingMode requestHandlingMode) {
        this.requestHandlingMode = requestHandlingMode;
    }
}
