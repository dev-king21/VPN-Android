package shenye.vpn.android.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Locale;

import butterknife.Bind;
import butterknife.OnClick;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;
import hu.blint.ssldroid.TcpProxyServerService;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import shenye.vpn.android.LaunchVPN;
import shenye.vpn.android.Preferences;
import shenye.vpn.android.R;
import shenye.vpn.android.ServerListAdapter;
import shenye.vpn.android.VPNhtApplication;
import shenye.vpn.android.VpnProfile;
import shenye.vpn.android.activities.MainActivity;
import shenye.vpn.android.content.VPNHTConfig;
import shenye.vpn.android.fragment.dialog.ConnectingDialogFragment;
import shenye.vpn.android.fragment.dialog.DisconnectingDialogFragment;
import shenye.vpn.android.fragment.dialog.LoadingDialogFragment;
import shenye.vpn.android.fragment.dialog.RebootDialogFragment;
import shenye.vpn.android.network.IPService;
import shenye.vpn.android.network.VPNService;
import shenye.vpn.android.network.responses.Country;
import shenye.vpn.android.network.responses.Server;
import shenye.vpn.android.network.responses.ServersResponse;
import shenye.vpn.android.network.responses.User;
import shenye.vpn.android.utils.PingUtils;
import shenye.vpn.android.utils.PrefUtils;
import shenye.vpn.android.utils.ThreadUtils;
import shenye.vpn.android.widget.TouchableScrollView;
import timber.log.Timber;

public class VPNFragment extends BaseFragment implements VpnStatus.StateListener, ByteCountListener {

    private Boolean mShowsConnected = false;
    private VPNService.Client mVPNAPI;
    private ArrayList<Country> mCountries = new ArrayList<>();
    private ArrayList<Server> mServers = new ArrayList<>();
    public static ArrayList<Server> mServersConnected = new ArrayList<>();
    private User mUser;
    public static MainActivity mActivity;
    private Country ctry;
    private String localIp;
    private VpnStatus.ConnectionStatus mCurrentVPNState = VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;

    @Bind(R.id.scrollView)
    TouchableScrollView mScrollView;
    @Bind(R.id.connectedCard)
    CardView mConnectedCard;
    @Bind(R.id.connectCard)
    CardView mConnectCard;
    @Bind(R.id.speedCard)
    CardView mSpeedCard;
    @Bind(R.id.connectButton)
    Button mConnectButton;
    @Bind(R.id.disconnectButton)
    Button mDisconnectButton;
    @Bind(R.id.logoutButton)
    Button mLogoutButton;
    @Bind(R.id.sortServerButton)
    Button mSortServerButton;
    @Bind(R.id.locationSpinner)
    Spinner mLocationSpinner;
    @Bind(R.id.ipText)
    TextView mIPText;
    @Bind(R.id.locationText)
    TextView mLocationText;
    @Bind(R.id.inSpeedText)
    public TextView mInSpeedText;
    @Bind(R.id.outSpeedText)
    public TextView mOutSpeedText;
    @Bind(R.id.expiredLbl)
    public TextView mExpiredLbl;
    @Bind(R.id.ipLbl)
    public TextView mIpLbl;
    @Bind(R.id.locationLbl)
    public TextView mLocationLbl;
    @Bind(R.id.uidText)
    public TextView mUidText;
    @Bind(R.id.expiredText)
    public TextView mExpiredText;
    @Bind(R.id.flagView)
    public ImageView mFlagView;
    @Bind(R.id.serverLbl)
    public TextView mServerLbl;

    /*public Typeface mFont;*/
    private long connectedTime = 0;
    private long sessionTime = 0;
    private int rnd_change_time = 10;
    private int serverIndex = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();
        setFont();
        LoadingDialogFragment.show(mActivity.getSupportFragmentManager());
        onServerReady();

        VpnStatus.addStateListener(this);
        updateIPData();
    }

    private void setFont()
    {
        /*mFont = Typeface.createFromAsset(getActivity().getAssets(), "font/wts.ttf");
        mConnectButton.setTypeface(mFont);
        mDisconnectButton.setTypeface(mFont);
        mLogoutButton.setTypeface(mFont);
        mIPText.setTypeface(mFont);
        mLocationText.setTypeface(mFont);
        mExpiredLbl.setTypeface(mFont);
        mIpLbl.setTypeface(mFont);
        mLocationLbl.setTypeface(mFont);
        mServerLbl.setTypeface(mFont);
        mUidText.setTypeface(mFont);
        mExpiredText.setTypeface(mFont);
        mInSpeedText.setTypeface(mFont);
        mOutSpeedText.setTypeface(mFont);*/

        mConnectButton.setTextSize(20.f);
        mDisconnectButton.setTextSize(20.f);
        mLogoutButton.setTextSize(20.f);
        mIPText.setTextSize(18.f);
        mLocationText.setTextSize(18.f);
        mExpiredLbl.setTextSize(18.f);
        mIpLbl.setTextSize(18.f);
        mLocationLbl.setTextSize(18.f);
        mInSpeedText.setTextSize(18.f);
        mOutSpeedText.setTextSize(18.f);
        mUidText.setTextSize(22.f);
        mExpiredText.setTextSize(18.f);
        mServerLbl.setTextSize(22.f);
        mExpiredText.setText("0000-00-00");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VpnStatus.removeStateListener(this);
    }

    @OnClick(R.id.connectButton)
    public void connectClick(View v) {
        ConnectingDialogFragment.show(getChildFragmentManager());

        final int spinnerPosition = mLocationSpinner.getSelectedItemPosition();

        if(mCountries.size() < spinnerPosition || spinnerPosition < 0) {
            try {
                Snackbar.make(mScrollView, R.string.no_server_selected, Snackbar.LENGTH_SHORT).show();
            } catch (RuntimeException e) {}
            ConnectingDialogFragment.dismiss(getChildFragmentManager());
        }

        VpnStatus.clearLog();
        ctry = mCountries.get(spinnerPosition);
        mServersConnected.clear();
        int i = 0;
        for(Server srv : mServers)
            if (srv.getCountry() == ctry.id)
            {
                srv.setListenerPort(1194 + i++);
                mServersConnected.add(srv);
            }
        mActivity.getStealthService().stopProxy();
        proxyServiceStart();
        startOpenVpn();
    }

    @OnClick(R.id.sortServerButton)
    public void sortServerClick(View v) {
        serverListShow();
    }

    private void setServerIndexRandom()
    {
        if (mServersConnected.size() > 1)
            while (true)
            {
                int idx = (int) Math.round(Math.random() * (mServersConnected.size() - 1));
                if (idx != serverIndex)
                {
                    serverIndex = idx;
                    break;
                }
            }
        else
            serverIndex = 0;
    }

    private void startOpenVpn()
    {
        setServerIndexRandom();
        final Server server = mServersConnected.get(serverIndex);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                int i = 0;
                while (true) {
                    try {
                        Thread.sleep(200);
                        if (i++ == 50)
                        {
                            ConnectingDialogFragment.dismiss(getChildFragmentManager());
                            DisconnectingDialogFragment.dismiss(getChildFragmentManager());
                            return null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (TcpProxyServerService.TUNNEL_CREATED) {
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_HOSTNAME, server.getHostname());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_MASTER, server.getMaster());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_NETMASK, server.getMask());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_ID, server.getId());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_CA, server.getCa());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_CRT, server.getCrt());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_KEY, server.getKey());
                        PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_FIREWALL, true);
                        ConfigParser configParser = new ConfigParser();

                        try {
                            configParser.parseConfig(new StringReader(VPNHTConfig.generate(PrefUtils.getPrefs(mActivity), server)));
                            VpnProfile profile = configParser.convertProfile();
                            profile.mUsername = PrefUtils.get(mActivity, Preferences.USERNAME, "");
                            profile.mPassword = PrefUtils.get(mActivity, Preferences.PASSWORD, "");
                            profile.mAuthenticationType = VpnProfile.TYPE_USERPASS_CERTIFICATES;
                            ProfileManager.setTemporaryProfile(profile);

                            Intent vpnPermissionIntent = VpnService.prepare(mActivity);

                            if (vpnPermissionIntent != null) {
                                Intent intent = new Intent(mActivity, LaunchVPN.class);
                                intent.setAction(Intent.ACTION_MAIN);
                                intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
                                intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
                                startActivity(intent);
                            } else {
                                VPNLaunchHelper.startOpenVpn(profile, VPNhtApplication.getAppContext());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ConfigParser.ConfigParseError configParseError) {
                            configParseError.printStackTrace();
                        }
                        return null;
                    }
                }
            }
        }.execute();
    }

    private void proxyServiceStart()
    {
        /*if (mActivity.getStealthService() != null)
            mActivity.getStealthService().stopProxy();*/
        mActivity.startService(new Intent(mActivity, TcpProxyServerService.class));
    }

    @OnClick(R.id.disconnectButton)
    public void diconnectClick(View v) {
        DisconnectingDialogFragment.show(getChildFragmentManager());
        mActivity.getVpnService().getManagement().stopVPN();
        mActivity.getStealthService().stopProxy();
    }

    private void AutoChangeServer()
    {
        mActivity.getVpnService().getManagement().stopVPN();
        startOpenVpn();
    }

    private void updateSession()
    {
        try {
            mVPNAPI.update_session(
                    mUser.username,
                    InetAddress.getLocalHost().getHostAddress(),
                    localIp,
                    mLocationText.getText().toString(),
                    "Android_" + Build.VERSION.SDK_INT,
                    mServersConnected.get(serverIndex).getCountry(),
                    sessionResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Callback<Response> sessionResponse = new Callback<Response>() {
        public void success(final Response response, Response response2) {
        }

        @Override
        public void failure(RetrofitError error) {
        }

    };

    @OnClick(R.id.logoutButton)
    public void logoutClick(View v) {
        if (mActivity != null)
            mActivity.startLoginActivity();
    }

    private void updateIPData() {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            mLocationText.setText(R.string.loading);
            mIPText.setText(R.string.loading);
            IPService.get().status(mIPCallback);
            }
        });

    }

    private void onServerReady() {
        if(mActivity == null || PrefUtils.get(mActivity, Preferences.USERNAME, "").equals("") || PrefUtils.get(mActivity, Preferences.PASSWORD, "").equals("")) return;
        String uname = PrefUtils.get(mActivity, Preferences.USERNAME, "");
        String upass = PrefUtils.get(mActivity, Preferences.PASSWORD, "");
        mVPNAPI = VPNService.get(uname, upass);
        mVPNAPI.servers(mServersCallback);
    }

    private Callback<IPService.Data> mIPCallback = new Callback<IPService.Data>() {
        @Override
        public void success(final IPService.Data data, Response response) {
            if(mActivity == null) return;

            if(response != null && response.getStatus() == 200) {
                if (mShowsConnected) {
                    mConnectedCard.setVisibility(View.VISIBLE);
                } else {
                    mConnectedCard.setVisibility(View.GONE);
                }

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    if (!TcpProxyServerService.TUNNEL_CREATED)
                        localIp = data.query;
                    mIPText.setText(data.query.replaceAll("[\\.]", "\\. "));
                    mLocationText.setText(String.format(Locale.US, "%s, %s", data.city, data.country));
                    int resID = mActivity.getResources().getIdentifier(data.countryCode.toLowerCase(), "drawable",  mActivity.getPackageName());
                    Log.d("shenye flag res ID", data.countryCode + " " + String.valueOf(resID));
                    mFlagView.setImageResource(resID);
                    }
                });
            }
        }

        @Override
        public void failure(RetrofitError error) {
            if (error.getResponse() != null && error.getResponse().getStatus() == 401) {
                mActivity.startLoginActivity();
            } else if (error.getCause() instanceof SocketTimeoutException){
                updateIPData();
            }
        }
    };

    private Callback<ServersResponse> mServersCallback = new Callback<ServersResponse>() {
        @Override
        public void success(ServersResponse serversResponse, Response response) {
            if(mActivity == null) return;
            mUser = serversResponse.user;
            mUidText.setText(mUser.username);
            mExpiredText.setText(mUser.expired_date.split(" ")[0]);
            mCountries = (ArrayList<Country>) serversResponse.countries;
            mServers = (ArrayList<Server>) serversResponse.servers;

            serverListShow();
        }

        @Override
        public void failure(RetrofitError error) {
            Timber.e(error, error.getMessage());
            if(error.getResponse() != null && error.getResponse().getStatus() == 401)
                Toast.makeText(mActivity, R.string.response_401, Toast.LENGTH_LONG).show();
            else if(error.getResponse() != null && error.getResponse().getStatus() == 402)
                Toast.makeText(mActivity, R.string.response_402, Toast.LENGTH_LONG).show();
            else if(error.getResponse() != null && error.getResponse().getStatus() == 409)
                Toast.makeText(mActivity, R.string.response_409, Toast.LENGTH_LONG).show();
            else if(error.getResponse() != null && error.getResponse().getStatus() == 423)
                Toast.makeText(mActivity, R.string.response_423, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(mActivity, R.string.response_unknown, Toast.LENGTH_LONG).show();

            mActivity.startLoginActivity();
        }
    };

    public void serverListShow() {
        mSortServerButton.setEnabled(false);
        mCountries = PingUtils.sortByPing(mCountries, mActivity);
        String[] spinnerList = new String[mCountries.size()];
        String[] ctCodeList = new String[mCountries.size()];
        String[] pingTimes = new String[mCountries.size()];
        int idx = 0;
        for(Country ctry: mCountries) {
            spinnerList[idx] = ctry.country;
            ctCodeList[idx] = ctry.code;
            if (ctry.pingTime == 1000000)
                pingTimes[idx++] = "--";
            else
                pingTimes[idx++] = ctry.pingTime + " ms";
        }

        final ServerListAdapter mLocationAdapter = new ServerListAdapter(mActivity, spinnerList, ctCodeList, pingTimes);
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocationSpinner.setAdapter(mLocationAdapter);
                LoadingDialogFragment.dismiss(mActivity.getSupportFragmentManager());
                mSortServerButton.setEnabled(true);
            }
        });
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {

        if (mServersConnected.size() > 1){
            if (sessionTime == 0)
                sessionTime = System.currentTimeMillis();
            if (connectedTime == 0)
            {
                connectedTime = System.currentTimeMillis();
                rnd_change_time = (int) Math.round(Math.random() * 5 + 10);
            }

            int elapsed_minute = Math.round((System.currentTimeMillis() - connectedTime) / 60000.f);
            long time_sec = ( (System.currentTimeMillis() - sessionTime)/ 1000 ) % 5;

            if (elapsed_minute >= rnd_change_time)
            {
                connectedTime = 0;
                AutoChangeServer();
                return;
            }

            if (time_sec >= 1)
            {
                sessionTime = 0;
                updateSession();
            }

        }

        final String netstat_in = String.format(getString(R.string.status_in_bytecount),
                OpenVPNService.humanReadableByteCount(in, false),
                OpenVPNService.humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true));
        final String netstat_out = String.format(getString(R.string.status_out_bytecount),
                OpenVPNService.humanReadableByteCount(out, false),
                OpenVPNService.humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true));
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInSpeedText.setText(netstat_in);
                mOutSpeedText.setText(netstat_out);
            }
        });
    }

    @Override
    public void updateState(final String state, String logmessage, int localizedResId, final VpnStatus.ConnectionStatus level) {
        if(mCurrentVPNState.equals(level))
            return;

        mCurrentVPNState = level;

        if(level.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTED) || level.equals(VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED)) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateIPData();
                }
            }, 500);
        }

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(level.equals(VpnStatus.ConnectionStatus.LEVEL_CONNECTED)) {
                    mConnectButton.setVisibility(View.GONE);
                    mLogoutButton.setVisibility(View.GONE);
                    mConnectedCard.setVisibility(View.VISIBLE);
                    mConnectCard.setVisibility(View.GONE);
                    mDisconnectButton.setVisibility(View.VISIBLE);
                    mSpeedCard.setVisibility(View.VISIBLE);
                    VpnStatus.addByteCountListener(VPNFragment.this);
                    ConnectingDialogFragment.dismiss(getChildFragmentManager());
                    DisconnectingDialogFragment.dismiss(getChildFragmentManager());
                } else if(level.equals(VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED)) {
                    mConnectedCard.setVisibility(View.GONE);
                    mDisconnectButton.setVisibility(View.GONE);
                    mConnectButton.setVisibility(View.VISIBLE);
                    mConnectCard.setVisibility(View.VISIBLE);
                    mLogoutButton.setVisibility(View.VISIBLE);
                    mSpeedCard.setVisibility(View.GONE);
                    ConnectingDialogFragment.dismiss(getChildFragmentManager());
                    DisconnectingDialogFragment.dismiss(getChildFragmentManager());
                    VpnStatus.removeByteCountListener(VPNFragment.this);
                    if(VpnStatus.getlogbuffer().length > 10) {
                        for(int i = VpnStatus.getlogbuffer().length - 10; i < VpnStatus.getlogbuffer().length; i++) {
                            VpnStatus.LogItem log = VpnStatus.getlogbuffer()[i];
                            String logString = log.getString(mActivity);
                            if (logString.contains("Cannot open TUN")) {
                                RebootDialogFragment.show(getChildFragmentManager());
                                break;
                            }
                        }
                    }
                } else {
                    ConnectingDialogFragment.show(getChildFragmentManager());
                }
            }
        });
    }

}
