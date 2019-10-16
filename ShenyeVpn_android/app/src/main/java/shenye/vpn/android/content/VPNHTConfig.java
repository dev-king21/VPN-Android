package shenye.vpn.android.content;

import android.content.SharedPreferences;

import java.util.ArrayList;

import shenye.vpn.android.Preferences;
import shenye.vpn.android.network.responses.Server;

public class VPNHTConfig {

    public static String generate(SharedPreferences preferences, Server server) {
        ArrayList<String> stringList = new ArrayList<>();

        stringList.add("client");
        stringList.add("dev tun");
        stringList.add("proto tcp");
        stringList.add("resolv-retry infinite");
        stringList.add("nobind");
        stringList.add("reneg-sec 0");
        stringList.add("tun-mtu 1500");
        stringList.add("tun-mtu-extra 32");
        stringList.add("mssfix 1450");
        stringList.add("persist-key");
        stringList.add("persist-tun");
        stringList.add("ping 15");
        stringList.add("ping-restart 45");
        stringList.add("ping-timer-rem");
        stringList.add("mute 10");
        stringList.add("verb 3");
        stringList.add("pull");
        stringList.add("fast-io");
        stringList.add("auth-nocache");
        stringList.add("remote-random");
        stringList.add("remote 127.0.0.1 "+server.getListenerPort());

        stringList.add(String.format("cipher %s", "AES-128-CBC"));
        stringList.add("<ca>");
        stringList.add("-----BEGIN CERTIFICATE-----");
        String[] caStr = preferences.getString(Preferences.LAST_CONNECTED_CA, "").split("\r\n");
        for (int i = 0; i< caStr.length; i++)
            stringList.add(caStr[i]);
        stringList.add("-----END CERTIFICATE-----");
        stringList.add("</ca>");

        stringList.add("<cert>");
        stringList.add("-----BEGIN CERTIFICATE-----");
        String[] crtStr = preferences.getString(Preferences.LAST_CONNECTED_CRT, "").split("\r\n");
        for (int i = 0; i< crtStr.length; i++)
            stringList.add(crtStr[i]);
        stringList.add("-----END CERTIFICATE-----");
        stringList.add("</cert>");


        stringList.add("<key>");
        stringList.add("-----BEGIN PRIVATE KEY-----");
        String[] keyStr = preferences.getString(Preferences.LAST_CONNECTED_KEY, "").split("\r\n");
        for (int i = 0; i< keyStr.length; i++)
            stringList.add(keyStr[i]);
        stringList.add("-----END PRIVATE KEY-----");
        stringList.add("</key>");


        StringBuilder builder = new StringBuilder();
        for(String s : stringList) {
            builder.append(s);
            builder.append('\n');
        }
        return builder.toString();
    }

}
