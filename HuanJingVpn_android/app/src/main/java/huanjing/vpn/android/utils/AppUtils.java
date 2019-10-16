package huanjing.vpn.android.utils;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

import hu.blint.ssldroid.TcpProxyServerService;

/*
 * Created by troy379 on 04.04.17.
 */
public class AppUtils {

    public static void showToast(Context context, @StringRes int text, boolean isLong) {
        showToast(context, context.getString(text), isLong);
    }

    public static void showToast(Context context, String text, boolean isLong) {
        Toast.makeText(context, text, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    public static void showToastSSLdroid(String text){
        Toast.makeText(TcpProxyServerService.mService, text, Toast.LENGTH_LONG).show();
    }

}