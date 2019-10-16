package hu.blint.ssldroid;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import huanjing.vpn.android.Preferences;
import java.util.ArrayList;
import huanjing.vpn.android.fragment.VPNFragment;
import huanjing.vpn.android.network.responses.Server;
import huanjing.vpn.android.utils.PrefUtils;


public class TcpProxyServerService extends Service {

    public static TcpProxyServerService mService;
    public static final String START_SERVICE = "tcp_proxy_start_service";
    private final IBinder mBinder = new LocalBinder();
    private static ArrayList<TcpProxy> tcpProxies;
    public static boolean TUNNEL_CREATED = false;

    public static ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String p12file = getApplicationContext().getCacheDir().getAbsolutePath() + "/" + "stunnel.p12";
        String pwd = PrefUtils.get(this, Preferences.PASSWORD, "");

        try {
            //stopProxy();
            tcpProxies = new ArrayList<>();
            int i = 1;
            for (Server srv : VPNFragment.mServersConnected)
            {
                boolean is_last = false;
                if (i++ == VPNFragment.mServersConnected.size())
                    is_last = true;
                TcpProxy ssl_proxy = new TcpProxy( srv.getAccountName(), srv.getListenerPort(), srv.getHostname(), srv.getPort(), p12file, pwd, is_last);
                tcpProxies.add(ssl_proxy);
                ssl_proxy.serve();
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("SSLDroid encountered a fatal error: "+e.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(START_SERVICE))
            return mBinder;
        return null;
    }

    public static void stopProxy()
    {
        TUNNEL_CREATED = false;
        if (tcpProxies == null || tcpProxies.size() == 0) return;
        for (TcpProxy ssl_proxy: tcpProxies)
            ssl_proxy.stop();
        tcpProxies = null;
    }

    public class LocalBinder extends Binder {
        public TcpProxyServerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TcpProxyServerService.this;
        }
    }

}

