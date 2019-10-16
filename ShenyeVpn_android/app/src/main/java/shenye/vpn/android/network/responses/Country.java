package shenye.vpn.android.network.responses;

public class Country implements Comparable<Country> {
    public int id;
    public String country;
    public String code;
    public String hostname;
    public int pingTime = 1000000;

    @Override
    public int compareTo(Country candidate) {
        return Integer.valueOf(pingTime).compareTo(candidate.pingTime) ;
    }
}
