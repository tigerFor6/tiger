package com.wisdge.cloud.configurer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MeteorConfig {

    @Value("${chinaamc.meteor.clientCode}")
    private String clientCode;
    @Value("${chinaamc.meteor.clientToken}")
    private String clientToken;
    @Value("${chinaamc.meteor.clientHost}")
    private String clientHost;
    @Value("${chinaamc.meteor.clientPort}")
    private int clientPort;
    @Value("${chinaamc.meteor.serverHost}")
    private String serverHost;
    @Value("${chinaamc.meteor.serverPort}")
    private int serverPort;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
