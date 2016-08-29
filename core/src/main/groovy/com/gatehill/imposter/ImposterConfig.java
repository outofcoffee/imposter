package com.gatehill.imposter;

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
    private String[] pluginClassNames;

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

    public void setPluginClassNames(String[] pluginClassNames) {
        this.pluginClassNames = pluginClassNames;
    }

    public String[] getPluginClassNames() {
        return pluginClassNames;
    }
}
