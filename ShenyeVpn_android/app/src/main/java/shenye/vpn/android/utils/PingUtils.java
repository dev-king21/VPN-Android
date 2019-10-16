package shenye.vpn.android.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

import shenye.vpn.android.network.responses.Country;
import timber.log.Timber;

public class PingUtils {

    public static int pingThreadCount;

    public static ArrayList<Country> sortByPing(ArrayList<Country> ctryList, Context ctx) {
        try {
            long start = System.currentTimeMillis();
            pingThreadCount = 0;
            final Context context = ctx;
            for (final Country ctry : ctryList) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        ctry.pingTime = ping(context, ctry.hostname);
                        pingThreadCount++;
                    }
                };
                thread.start();
            }
            while (pingThreadCount != ctryList.size()) Thread.sleep(100);
            long end = System.currentTimeMillis();
            Log.d("PingTime:", String.valueOf(end-start)+"ms");
        } catch (Exception e) {

        }
        return sort(ctryList);
    }

    public static int ping(Context ctx, String hostname) {
        if (isNetworkConnected(ctx)) {
            //String net = getNetworkType(ctx);
            try {
                Socket socket = new Socket();
                InetSocketAddress address = new InetSocketAddress(InetAddress.getAllByName(hostname)[0], 443);
                long start = System.currentTimeMillis();
                socket.connect(address, 1000);
                long probeFinish = System.currentTimeMillis();
                socket.close();

                return (int) (probeFinish - start);
            }
            catch (Exception ex) {
                Timber.e("Unable to ping");
            }
        }
        return 1000000;
    }

    public static ArrayList<Country> sort(ArrayList<Country> arrayList) {
        Collections.sort(arrayList);
        return arrayList;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Nullable
    public static String getNetworkType(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            return activeNetwork.getTypeName();
        }
        return null;
    }
}
