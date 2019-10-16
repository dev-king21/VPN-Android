package shenye.vpn.android.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Locale;

import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;
import hu.blint.ssldroid.TcpProxyServerService;
import shenye.vpn.android.Preferences;
import shenye.vpn.android.R;
import shenye.vpn.android.fragment.VPNFragment;
import shenye.vpn.android.utils.NetworkUtils;
import shenye.vpn.android.utils.PrefUtils;

public class MainActivity extends BaseActivity implements VpnStatus.StateListener {

    private OpenVPNService mService;
    private Boolean mConnected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(!PrefUtils.contains(this, Preferences.USERNAME) || !PrefUtils.contains(this, Preferences.PASSWORD)) {
            startLoginActivity();
        }

        String languageToLoad = PrefUtils.get(this, Preferences.LANGUAGE, "");
        Locale.setDefault(new Locale(languageToLoad));
        Configuration config = new Configuration();
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        super.onCreate(savedInstanceState, R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Intent intent1 = new Intent(this, TcpProxyServerService.class);
        intent1.setAction(TcpProxyServerService.START_SERVICE);
        bindService(intent1, TcpProxyServerService.mConnection, Context.BIND_AUTO_CREATE);

        if(!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
        unbindService(TcpProxyServerService.mConnection);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_prefs).setVisible(!mConnected);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_prefs:
                Intent prefsIntent = new Intent(this, PreferencesActivity.class);
                startActivity(prefsIntent);
                break;
            case R.id.action_log:
                Intent logIntent = new Intent(this, LogActivity.class);
                startActivity(logIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
*/
    public void startLoginActivity() {
        PrefUtils.remove(this, Preferences.USERNAME);
        PrefUtils.remove(this, Preferences.PASSWORD);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    public OpenVPNService getVpnService() {
        return mService;
    }

    public TcpProxyServerService getStealthService() {
        return TcpProxyServerService.mService;
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        mConnected = level.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTED);
    }
}
