package shenye.vpn.android;

import android.app.Application;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import shenye.vpn.android.utils.PrefUtils;

public class VPNhtApplication extends Application implements VpnStatus.StateListener {

    private static VPNhtApplication sThis;
    private boolean mCreated = false;

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
        sThis = this;
        VpnStatus.addStateListener(this);
    }

    public static VPNhtApplication getAppContext() {
        return sThis;
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        if(!mCreated) {
            mCreated = true;
            return;
        }

        if(level.equals(VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED)) {
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_HOSTNAME);
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_FIREWALL);
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_COUNTRY);
        }
    }
}
