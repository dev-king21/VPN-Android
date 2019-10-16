package huanjing.vpn.android.network.responses;


public class Server {

    private int id;
    private String AccountName;
    private String Hostname;
    private String HubName;
    private int Port;
    private String ca;
    private String crt;
    private String key;
    private int Country;
    private int listenerPort;
    private String Master;
    private String Mask;

    public Server() {

    }

    public Server(int id, String accountName, String hostname, String mask, String hubName, int port) {
        this.id = id;
        AccountName = accountName;
        Hostname = hostname;
        Mask = mask;
        HubName = hubName;
        Port = port;
    }

    public int getId() {
        return id;
    }

    public String getAccountName() {
        return AccountName;
    }

    public String getHostname() {
        return Hostname;
    }

    public String getHubName() {
        return HubName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAccountName(String accountName) {
        AccountName = accountName;
    }

    public void setHostname(String hostname) {
        Hostname = hostname;
    }

    public void setHubName(String hubName) {
        HubName = hubName;
    }
    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }

    public String getCa() {
        return ca;
    }

    public void setCa(String ca) {
        this.ca = ca;
    }

    public String getCrt() {
        return crt;
    }

    public void setCrt(String crt) {
        this.crt = crt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getCountry() {
        return Country;
    }

    public void setCountry(int country) {
        Country = country;
    }

    public String getMask() {
        return Mask;
    }

    public void setMask(String mask) {
        Mask = mask;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public void setListenerPort(int listenerPort) {
        this.listenerPort = listenerPort;
    }

    public String getMaster() {
        return Master;
    }

    public void setMaster(String master) {
        Master = master;
    }
}

