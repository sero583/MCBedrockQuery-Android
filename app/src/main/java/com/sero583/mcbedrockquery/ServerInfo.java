package com.sero583.mcbedrockquery;

public class ServerInfo {
    public static final int DEFUALT_PORT = 19132;
    private String ip;
    private int port;

    public ServerInfo(String ip) {
        this(ip, DEFUALT_PORT);
    }

    public ServerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
