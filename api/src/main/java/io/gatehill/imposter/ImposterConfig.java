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
