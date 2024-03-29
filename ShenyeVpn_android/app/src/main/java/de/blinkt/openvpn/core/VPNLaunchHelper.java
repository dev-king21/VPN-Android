/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

import shenye.vpn.android.Preferences;
import shenye.vpn.android.R;
import shenye.vpn.android.VpnProfile;
import shenye.vpn.android.utils.PrefUtils;

public class VPNLaunchHelper {
    private static final String MININONPIEVPN = "nopie_openvpn";
    private static final String MINIPIEVPN = "pie_openvpn";
    private static final String OVPNCONFIGFILE = "android%s.conf";



    static private String writeMiniVPN(Context context) {
        String[] abis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            abis = getSupportedAbisLollipop();
        else
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};

        for (String abi: abis) {

            File mvpnout = new File(context.getCacheDir(), getMiniVPNExecutableName() + "." + abi);
            if ((mvpnout.exists() && mvpnout.canExecute()) || writeMiniVPNBinary(context, abi, mvpnout)) {
                return mvpnout.getPath();
            }
        }

        return null;
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String[] getSupportedAbisLollipop() {
        return Build.SUPPORTED_ABIS;
    }

    private static String getMiniVPNExecutableName()
    {
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.JELLY_BEAN)
            return MINIPIEVPN;
        else
            return MININONPIEVPN;
    }


    public static String[] replacePieWithNoPie(String[] mArgv)
    {
        mArgv[0] = mArgv[0].replace(MINIPIEVPN, MININONPIEVPN);
        return mArgv;
    }


    public static String[] buildOpenvpnArgv(Context c) {
        Vector<String> args = new Vector<>();
        args.add(writeMiniVPN(c));
        args.add("--config");
        args.add(getConfigFilePath(c));

        return args.toArray(new String[args.size()]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open(getMiniVPNExecutableName() + "." + abi);
            }
            catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for archicture " + abi);
                return false;
            }

            FileOutputStream fout = new FileOutputStream(mvpnout);
            byte buf[]= new byte[4096];
            int lenread = mvpn.read(buf);
            while(lenread> 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if(!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }


            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        }

    }
	

	public static void startOpenVpn(VpnProfile startprofile, Context context) {
		if(writeMiniVPN(context)==null) {
			VpnStatus.logError("Error writing minivpn binary");
			return;
		}

		VpnStatus.logInfo(R.string.building_configration);

		Intent startVPN = startprofile.prepareStartService(context);
		if(startVPN!=null)
			context.startService(startVPN);

	}

    public static String getConfigFilePath(Context context) {
        String serverId = PrefUtils.get(context, Preferences.LAST_CONNECTED_ID, "1");
        return context.getCacheDir().getAbsolutePath() + "/" + String.format(Locale.US, OVPNCONFIGFILE, serverId);
    }

}
