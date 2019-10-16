package shenye.vpn.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import shenye.vpn.android.content.ObscuredSharedPreferences;
import shenye.vpn.android.utils.PrefUtils;
import java.util.Map;

public class Preferences {
    public static final String ENC_TYPE = "encryption";
    @Deprecated
    public static final String FILE_NAME = "vpn.ht";
    public static final String KILLSWITCH = "killswitch";
    public static final String LAST_CONNECTED_COUNTRY = "country_latest";
    public static final String LAST_CONNECTED_FIREWALL = "firewall_last";
    public static final String LAST_CONNECTED_HOSTNAME = "hostname_last";
    public static final String LAST_CONNECTED_MASTER = "master_host";
    public static final String LAST_CONNECTED_PORT = "port";
    public static final String LAST_CONNECTED_NETMASK = "net_mask";
    public static final String LAST_CONNECTED_ID = "server_id";
    public static final String LAST_CONNECTED_CA = "ca_last";
    public static final String LAST_CONNECTED_CRT = "crt_last";
    public static final String LAST_CONNECTED_KEY = "key_last";
    public static final String LOG_VERBOSITY = "log_verbosity";
    public static final String PASSWORD = "password";
    public static final String RECONNECT_NETCHANGE = "netchangereconnect";
    public static final String RECONNECT_REBOOT = "rebootreconnect";
    public static final String SMARTDNS = "smartdns";
    public static final String USERNAME = "username";
    public static final String CA = "ca";
    public static final String CRT = "crt";
    public static final String KEY = "key";
    public static final String LANGUAGE = "lang";

    @Deprecated
    public static void movePreferences(Context context, String source) {
        SharedPreferences oldPrefs = context.getSharedPreferences(source, 0);
        ObscuredSharedPreferences newPrefs = PrefUtils.getPrefs(context);
        Map<String, ?> map = oldPrefs.getAll();
        Editor editor = newPrefs.edit();
        for (String key : map.keySet()) {
            Object item = map.get(key);
            if (item instanceof Boolean) {
                editor.putBoolean(key, ((Boolean) item).booleanValue());
            } else if (item instanceof String) {
                editor.putString(key, (String) item);
            } else if (item instanceof Integer) {
                editor.putInt(key, ((Integer) item).intValue());
            }
        }
        editor.apply();
        oldPrefs.edit().clear().apply();
    }
}