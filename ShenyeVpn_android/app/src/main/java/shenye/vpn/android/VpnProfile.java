/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package shenye.vpn.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.text.TextUtils;
import android.util.Base64;

import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.NativeUtils;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.X509Utils;
import shenye.vpn.android.utils.PrefUtils;

public class VpnProfile implements Serializable, Cloneable {
    // Note that this class cannot be moved to core where it belongs since
    // the profile loading depends on it being here
    // The Serializable documentation mentions that class name change are possible
    // but the how is unclear
    //
    public static final int CURRENT_PROFILE_VERSION = 5;
    public static String DEFAULT_DNS1 = "8.8.8.8";
    public static String DEFAULT_DNS2 = "8.8.4.4";
    public static final int DEFAULT_MSSFIX_SIZE = 1450;
    public static final String DISPLAYNAME_TAG = "[[NAME]]";
    public static final String INLINE_TAG = "[[INLINE]]";
    public static final int TYPE_CERTIFICATES = 0;
    public static final int TYPE_KEYSTORE = 2;
    public static final int TYPE_PKCS12 = 1;
    public static final int TYPE_STATICKEYS = 4;
    public static final int TYPE_USERPASS = 3;
    public static final int TYPE_USERPASS_CERTIFICATES = 5;
    public static final int TYPE_USERPASS_KEYSTORE = 7;
    public static final int TYPE_USERPASS_PKCS12 = 6;
    private static final long serialVersionUID = 7085688938959334563L;
    public String mAlias;
    public boolean mAllowLocalLAN;
    public HashSet<String> mAllowedAppsVpn = new HashSet();
    public boolean mAllowedAppsVpnAreDisallowed = true;
    public String mAuth = "SHA1";
    public int mAuthenticationType = 2;
    public String mCaFilename;
    public boolean mCheckRemoteCN = true;
    public String mCipher = "";
    public String mClientCertFilename;
    public String mClientKeyFilename;
    public String mConnectRetry = "5";
    public String mConnectRetryMax = "5";
    public Connection[] mConnections = new Connection[0];
    public String mCustomConfigOptions = "";
    public String mCustomRoutes;
    public String mCustomRoutesv6 = "";
    public String mExcludedRoutes;
    /*public boolean mExpectTLSCert = false;*/
    public String mIPv4Address;
    public String mKeyPassword = "";
    public int mMssFix = 0;
    public String mName;
    public boolean mNobind = false;
    public boolean mOverrideDNS = false;
    public String mPKCS12Filename;
    public String mPKCS12Password;
    public String mPassword = "";
    public boolean mPersistTun = false;
    private transient PrivateKey mPrivateKey;
    public String mProfileCreator;
    private int mProfileVersion;
    public String mRemoteCN = "";
    public boolean mRemoteRandom = false;
    public String mSearchDomain = "blinkt.de";
    public String mServerName = "openvpn.blinkt.de";
    public String mServerPort = "1194";
    public String mTLSAuthDirection = "";
    public String mTLSAuthFilename;
    public transient String mTransientPCKS12PW = null;
    public transient String mTransientPW = null;
    public boolean mUseCustomConfig = false;
    public boolean mUseDefaultRoute = true;
    public boolean mUseDefaultRoutev6 = true;
    public boolean mUseFloat = false;
    public boolean mUseLzo = false;
    public boolean mUsePull = true;
    public boolean mUseRandomHostname = false;
    public boolean mUseTLSAuth = false;
    public boolean mUseUdp = true;
    public boolean mUserEditable = true;
    public String mUsername = "";
    private UUID mUuid = UUID.randomUUID();
    /*public String mVerb = "1";
    public int mX509AuthType = 3;*/
    public transient boolean profileDeleted = false;



    public VpnProfile(String name) {
        mUuid = UUID.randomUUID();
        mName = name;
        mProfileVersion = CURRENT_PROFILE_VERSION;

        mConnections = new Connection[1];
        mConnections[0] = new Connection();
    }

    public static String openVpnEscape(String unescaped) {
        if (unescaped == null)
            return null;
        String escapedString = unescaped.replace("\\", "\\\\");
        escapedString = escapedString.replace("\"", "\\\"");
        escapedString = escapedString.replace("\n", "\\n");

        if (escapedString.equals(unescaped) && !escapedString.contains(" ") &&
                !escapedString.contains("#") && !escapedString.contains(";")
                && !escapedString.equals(""))
            return unescaped;
        else
            return '"' + escapedString + '"';
    }

    public void clearDefaults() {
        mServerName = "unknown";
        mUsePull = false;
        mUseLzo = false;
        mUseDefaultRoute = false;
        mUseDefaultRoutev6 = false;
        mCheckRemoteCN = false;
        mPersistTun = false;
        mAllowLocalLAN = true;
        mMssFix = 0;
    }

    public UUID getUUID() {
        return mUuid;

    }

    public String getName() {
        if (mName == null)
            return "No profile name";
        return mName;
    }

    public void upgradeProfile() {
        if (mProfileVersion < 2) {
            /* default to the behaviour the OS used */
            mAllowLocalLAN = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        }

        if (mProfileVersion < 4) {
            moveOptionsToConnection();
            mAllowedAppsVpnAreDisallowed = true;
        }
        if (mAllowedAppsVpn == null)
            mAllowedAppsVpn = new HashSet<String>();
        if (mConnections == null)
            mConnections = new Connection[0];

        mProfileVersion = CURRENT_PROFILE_VERSION;

    }

    private void moveOptionsToConnection() {
        mConnections = new Connection[1];
        Connection conn = new Connection();

        conn.mServerName = mServerName;
        conn.mServerPort = mServerPort;
        conn.mUseUdp = mUseUdp;
        conn.mCustomConfiguration = "";

        mConnections[0] = conn;

    }

    public String getConfigFile(Context context, boolean configForOvpn3) {

        File cacheDir = context.getCacheDir();
        String cfg = "";

        cfg += "# Enables connection to GUI\n";
        cfg += "management " + cacheDir.getAbsolutePath() + "/" + "mgmtsocket unix\n";
        cfg += "management-client\n";
        cfg += "management-query-passwords\n";
        cfg += "management-hold\n\n";

        if (!configForOvpn3)
            cfg += String.format("setenv IV_GUI_VER %s \n", openVpnEscape(getVersionEnvString(context)));

        cfg += "machine-readable-output\n";
        cfg += "ifconfig-nowarn\n";
        cfg += "client\n";
        cfg += "verb " + 7 + "\n";
        cfg += "connect-retry-max 5\n";
        cfg += "connect-retry 5\n";
        cfg += "resolv-retry 60\n";
        cfg += "dev tun\n";

        boolean canUsePlainRemotes = true;

        if (mConnections.length == 1) {
            cfg += mConnections[0].getConnectionBlock();
        } else {
            for (Connection conn : mConnections) {
                canUsePlainRemotes = canUsePlainRemotes && conn.isOnlyRemote();
            }

            if (mRemoteRandom)
                cfg += "remote-random\n";

            if (canUsePlainRemotes) {
                for (Connection conn : mConnections) {
                    if (conn.mEnabled) {
                        cfg += conn.getConnectionBlock();
                    }
                }
            }
        }

        switch (mAuthenticationType) {
            case VpnProfile.TYPE_USERPASS_CERTIFICATES:
                cfg += "auth-user-pass\n";
            case VpnProfile.TYPE_CERTIFICATES:
                cfg += insertFileData("ca", mCaFilename);
                cfg += insertFileData("key", mClientKeyFilename);
                cfg += insertFileData("cert", mClientCertFilename);


                break;
            case VpnProfile.TYPE_USERPASS_PKCS12:
                cfg += "auth-user-pass\n";
            case VpnProfile.TYPE_PKCS12:
                cfg += insertFileData("pkcs12", mPKCS12Filename);
                break;

            case VpnProfile.TYPE_USERPASS_KEYSTORE:
                cfg += "auth-user-pass\n";
            case VpnProfile.TYPE_KEYSTORE:
                if (!configForOvpn3) {
                    String[] ks = getKeyStoreCertificates(context);
                    cfg += "### From Keystore ####\n";
                    if (ks != null) {
                        cfg += "<ca>\n" + ks[0] + "\n</ca>\n";
                        if (ks[1] != null)
                            cfg += "<extra-certs>\n" + ks[1] + "\n</extra-certs>\n";
                        cfg += "<cert>\n" + ks[2] + "\n</cert>\n";
                        cfg += "management-external-key\n";
                    } else {
                        cfg += context.getString(R.string.keychain_access) + "\n";
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN)
                            if (!mAlias.matches("^[a-zA-Z0-9]$"))
                                cfg += context.getString(R.string.jelly_keystore_alphanumeric_bug) + "\n";
                    }
                }
                break;
            case VpnProfile.TYPE_USERPASS:
                cfg += "auth-user-pass\n";
                cfg += insertFileData("ca", mCaFilename);
        }

        cfg += String.format(Locale.US, "route %s %s net_gateway\n",
                PrefUtils.get(context, Preferences.LAST_CONNECTED_HOSTNAME, ""),
                PrefUtils.get(context, Preferences.LAST_CONNECTED_NETMASK, "")
            );

        if (mMssFix != 0) {
            if (mMssFix != 1450)
                cfg += String.format(Locale.US, "mssfix %d\n", mMssFix);
            else
                cfg += "mssfix\n";
        }

        if (mNobind)
            cfg += "nobind\n";


        if (!TextUtils.isEmpty(mCipher)) {
            cfg += "cipher " + mCipher + "\n";
        }

        if (!TextUtils.isEmpty(mAuth)) {
            cfg += "auth " + mAuth + "\n";
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usesystemproxy = prefs.getBoolean("usesystemproxy", true);
        if (usesystemproxy) {
            cfg += "management-query-proxy\n";
        }


        if (mUseCustomConfig) {
            cfg += "# Custom configuration options\n";
            cfg += mCustomConfigOptions;
            cfg += "\n";

        }

        return cfg;
    }

    public String getVersionEnvString(Context c) {
        String version = "unknown";
        try {
            PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = packageinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            VpnStatus.logException(e);
        }
        return String.format(Locale.US, "%s %s", c.getPackageName(), version);

    }

    //! Put inline data inline and other data as normal escaped filename
    public static String insertFileData(String cfgentry, String filedata) {
        if (filedata == null) {
            // TODO: generate good error
            return String.format("%s %s\n", cfgentry, "missing");
        } else if (isEmbedded(filedata)) {
            String dataWithOutHeader = getEmbeddedContent(filedata);
            return String.format(Locale.ENGLISH, "<%s>\n%s\n</%s>\n", cfgentry, dataWithOutHeader, cfgentry);
        } else {
            return String.format(Locale.ENGLISH, "%s %s\n", cfgentry, openVpnEscape(filedata));
        }
    }

    private Collection<String> getCustomRoutes(String routes) {
        Vector<String> cidrRoutes = new Vector<String>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                String cidrroute = cidrToIPAndNetmask(route);
                if (cidrroute == null)
                    return null;

                cidrRoutes.add(cidrroute);
            }
        }

        return cidrRoutes;
    }

    private Collection<String> getCustomRoutesv6(String routes) {
        Vector<String> cidrRoutes = new Vector<String>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                cidrRoutes.add(route);
            }
        }

        return cidrRoutes;
    }

    private String cidrToIPAndNetmask(String route) {
        String[] parts = route.split("/");

        // No /xx, assume /32 as netmask
        if (parts.length == 1)
            parts = (route + "/32").split("/");

        if (parts.length != 2)
            return null;
        int len;
        try {
            len = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ne) {
            return null;
        }
        if (len < 0 || len > 32)
            return null;


        long nm = 0xffffffffl;
        nm = (nm << (32 - len)) & 0xffffffffl;

        String netmask = String.format(Locale.ENGLISH, "%d.%d.%d.%d", (nm & 0xff000000) >> 24, (nm & 0xff0000) >> 16, (nm & 0xff00) >> 8, nm & 0xff);
        return parts[0] + "  " + netmask;
    }


    public Intent prepareStartService(Context context) {
        Intent intent = getStartServiceIntent(context);


        if (mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
            if (getKeyStoreCertificates(context) == null)
                return null;
        }


        try {
            FileWriter cfg = new FileWriter(VPNLaunchHelper.getConfigFilePath(context));
            cfg.write(getConfigFile(context, false));
            cfg.flush();
            cfg.close();
        } catch (IOException e) {
            VpnStatus.logException(e);
        }

        return intent;
    }

    public Intent getStartServiceIntent(Context context) {
        String prefix = context.getPackageName();

        Intent intent = new Intent(context, OpenVPNService.class);
        intent.putExtra(prefix + ".ARGV", VPNLaunchHelper.buildOpenvpnArgv(context));
        intent.putExtra(prefix + ".profileUUID", mUuid.toString());

        ApplicationInfo info = context.getApplicationInfo();
        intent.putExtra(prefix + ".nativelib", info.nativeLibraryDir);
        return intent;
    }

    public String[] getKeyStoreCertificates(Context context) {
        return getKeyStoreCertificates(context, 5);
    }

    public static String getDisplayName(String embeddedFile) {
        int start = DISPLAYNAME_TAG.length();
        int end = embeddedFile.indexOf(INLINE_TAG);
        return embeddedFile.substring(start, end);
    }

    public static String getEmbeddedContent(String data) {
        if (!data.contains(INLINE_TAG))
            return data;

        int start = data.indexOf(INLINE_TAG) + INLINE_TAG.length();
        return data.substring(start);
    }

    public static boolean isEmbedded(String data) {
        if (data == null)
            return false;
        if (data.startsWith(INLINE_TAG) || data.startsWith(DISPLAYNAME_TAG))
            return true;
        else
            return false;
    }

    public void checkForRestart(final Context context) {
        /* This method is called when OpenVPNService is restarted */

        if ((mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE)
                && mPrivateKey == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getKeyStoreCertificates(context);

                }
            }).start();
        }
    }

    @Override
    protected VpnProfile clone() throws CloneNotSupportedException {
        VpnProfile copy = (VpnProfile) super.clone();
        copy.mUuid = UUID.randomUUID();
        copy.mConnections = new Connection[mConnections.length];
        int i = 0;
        for (Connection conn : mConnections) {
            copy.mConnections[i++] = conn.clone();
        }
        copy.mAllowedAppsVpn = (HashSet<String>) mAllowedAppsVpn.clone();
        return copy;
    }

    public VpnProfile copy(String name) {
        try {
            VpnProfile copy = (VpnProfile) clone();
            copy.mName = name;
            return copy;

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }


    class NoCertReturnedException extends Exception {
        public NoCertReturnedException(String msg) {
            super(msg);
        }
    }

    synchronized String[] getKeyStoreCertificates(Context context, int tries) {
        PrivateKey privateKey = null;
        X509Certificate[] caChain;
        Exception exp;
        try {
            privateKey = KeyChain.getPrivateKey(context, mAlias);
            mPrivateKey = privateKey;

            String keystoreChain = null;


            caChain = KeyChain.getCertificateChain(context, mAlias);
            if (caChain == null)
                throw new NoCertReturnedException("No certificate returned from Keystore");

            if (caChain.length <= 1 && TextUtils.isEmpty(mCaFilename)) {
                VpnStatus.logMessage(VpnStatus.LogLevel.ERROR, "", context.getString(R.string.keychain_nocacert));
            } else {
                StringWriter ksStringWriter = new StringWriter();

                PemWriter pw = new PemWriter(ksStringWriter);
                for (int i = 1; i < caChain.length; i++) {
                    X509Certificate cert = caChain[i];
                    pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                }
                pw.close();
                keystoreChain = ksStringWriter.toString();
            }


            String caout = null;
            if (!TextUtils.isEmpty(mCaFilename)) {
                try {
                    Certificate cacert = X509Utils.getCertificateFromFile(mCaFilename);
                    StringWriter caoutWriter = new StringWriter();
                    PemWriter pw = new PemWriter(caoutWriter);

                    pw.writeObject(new PemObject("CERTIFICATE", cacert.getEncoded()));
                    pw.close();
                    caout = caoutWriter.toString();

                } catch (Exception e) {
                    VpnStatus.logError("Could not read CA certificate" + e.getLocalizedMessage());
                }
            }


            StringWriter certout = new StringWriter();


            if (caChain.length >= 1) {
                X509Certificate usercert = caChain[0];

                PemWriter upw = new PemWriter(certout);
                upw.writeObject(new PemObject("CERTIFICATE", usercert.getEncoded()));
                upw.close();

            }
            String user = certout.toString();


            String ca, extra;
            if (caout == null) {
                ca = keystoreChain;
                extra = null;
            } else {
                ca = caout;
                extra = keystoreChain;
            }

            return new String[]{ca, extra, user};
        } catch (InterruptedException e) {
            exp = e;
        } catch (FileNotFoundException e) {
            exp = e;
        } catch (CertificateException e) {
            exp = e;
        } catch (IOException e) {
            exp = e;
        } catch (KeyChainException e) {
            exp = e;
        } catch (NoCertReturnedException e) {
            exp = e;
        } catch (IllegalArgumentException e) {
            exp = e;
        } catch (AssertionError e) {
            if (tries == 0)
                return null;
            VpnStatus.logError(String.format("Failure getting Keystore Keys (%s), retrying", e.getLocalizedMessage()));
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
                VpnStatus.logException(e1);
            }
            return getKeyStoreCertificates(context, tries - 1);
        }

        exp.printStackTrace();
        VpnStatus.logError(R.string.keyChainAccessError, exp.getLocalizedMessage());

        VpnStatus.logError(R.string.keychain_access);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            if (!mAlias.matches("^[a-zA-Z0-9]$")) {
                VpnStatus.logError(R.string.jelly_keystore_alphanumeric_bug);
            }
        }
        return null;
    }

    //! Return an error if somethign is wrong
    public int checkProfile() {
        if (mAuthenticationType == TYPE_KEYSTORE || mAuthenticationType == TYPE_USERPASS_KEYSTORE) {
            if (mAlias == null)
                return R.string.no_keystore_cert_selected;
        }

        if (!mUsePull || mAuthenticationType == TYPE_STATICKEYS) {
            if (mIPv4Address == null || cidrToIPAndNetmask(mIPv4Address) == null)
                return R.string.ipv4_format_error;
        }
        if (!mUseDefaultRoute && (getCustomRoutes(mCustomRoutes) == null || getCustomRoutes(mExcludedRoutes) == null))
            return R.string.custom_route_format_error;

        boolean noRemoteEnabled = true;
        for (Connection c : mConnections)
            if (c.mEnabled)
                noRemoteEnabled = false;

        if (noRemoteEnabled)
            return R.string.remote_no_server_selected;

        // Everything okay
        return R.string.no_error_found;

    }

    //! Openvpn asks for a "Private Key", this should be pkcs12 key
    //
    public String getPasswordPrivateKey() {
        if (mTransientPCKS12PW != null) {
            String pwcopy = mTransientPCKS12PW;
            mTransientPCKS12PW = null;
            return pwcopy;
        }
        switch (mAuthenticationType) {
            case TYPE_PKCS12:
            case TYPE_USERPASS_PKCS12:
                return mPKCS12Password;

            case TYPE_CERTIFICATES:
            case TYPE_USERPASS_CERTIFICATES:
                return mKeyPassword;

            case TYPE_USERPASS:
            case TYPE_STATICKEYS:
            default:
                return null;
        }
    }

    public boolean isUserPWAuth() {
        switch (mAuthenticationType) {
            case TYPE_USERPASS:
            case TYPE_USERPASS_CERTIFICATES:
            case TYPE_CERTIFICATES:
            case TYPE_USERPASS_KEYSTORE:
            case TYPE_USERPASS_PKCS12:
                return true;
            default:
                return false;

        }
    }

    public boolean requireTLSKeyPassword() {
        if (TextUtils.isEmpty(mClientKeyFilename))
            return false;

        String data = "";
        if (isEmbedded(mClientKeyFilename))
            data = mClientKeyFilename;
        else {
            char[] buf = new char[2048];
            FileReader fr;
            try {
                fr = new FileReader(mClientKeyFilename);
                int len = fr.read(buf);
                while (len > 0) {
                    data += new String(buf, 0, len);
                    len = fr.read(buf);
                }
                fr.close();
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

        }

        if (data.contains("Proc-Type: 4,ENCRYPTED"))
            return true;
        else if (data.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
            return true;
        else
            return false;
    }

    public int needUserPWInput(boolean ignoreTransient) {
        if ((mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12) &&
                (mPKCS12Password == null || mPKCS12Password.equals(""))) {
            if (ignoreTransient || mTransientPCKS12PW == null)
                return R.string.pkcs12_file_encryption_key;
        }

        if (mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES) {
            if (requireTLSKeyPassword() && TextUtils.isEmpty(mKeyPassword))
                if (ignoreTransient || mTransientPCKS12PW == null) {
                    return R.string.private_key_password;
                }
        }

        if (isUserPWAuth() &&
                (TextUtils.isEmpty(mUsername) ||
                        (TextUtils.isEmpty(mPassword) && (mTransientPW == null || ignoreTransient)))) {
            return R.string.password;
        }
        return 0;
    }

    public String getPasswordAuth() {
        if (mTransientPW != null) {
            String pwcopy = mTransientPW;
            mTransientPW = null;
            return pwcopy;
        } else {
            return mPassword;
        }
    }

    // Used by the Array Adapter
    @Override
    public String toString() {
        return mName;
    }

    public String getUUIDString() {
        return mUuid.toString();
    }

    public PrivateKey getKeystoreKey() {
        return mPrivateKey;
    }

    public String getSignedData(String b64data) {
        PrivateKey privkey = getKeystoreKey();
        Exception err;

        byte[] data = Base64.decode(b64data, Base64.DEFAULT);

        // The Jelly Bean *evil* Hack
        // 4.2 implements the RSA/ECB/PKCS1PADDING in the OpenSSLprovider
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            return processSignJellyBeans(privkey, data);
        }


        try {

            /* ECB is perfectly fine in this special case, since we are using it for
               the public/private part in the TLS exchange
             */
            @SuppressLint("GetInstance")
            Cipher rsaSigner = Cipher.getInstance("RSA/ECB/PKCS1PADDING");

            rsaSigner.init(Cipher.ENCRYPT_MODE, privkey);

            byte[] signed_bytes = rsaSigner.doFinal(data);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchAlgorithmException e) {
            err = e;
        } catch (InvalidKeyException e) {
            err = e;
        } catch (NoSuchPaddingException e) {
            err = e;
        } catch (IllegalBlockSizeException e) {
            err = e;
        } catch (BadPaddingException e) {
            err = e;
        }

        VpnStatus.logError(R.string.error_rsa_sign, err.getClass().toString(), err.getLocalizedMessage());

        return null;

    }

    private String processSignJellyBeans(PrivateKey privkey, byte[] data) {
        Exception err;
        try {
            Method getKey = privkey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey");
            getKey.setAccessible(true);

            // Real object type is OpenSSLKey
            Object opensslkey = getKey.invoke(privkey);

            getKey.setAccessible(false);

            Method getPkeyContext = opensslkey.getClass().getDeclaredMethod("getPkeyContext");

            // integer pointer to EVP_pkey
            getPkeyContext.setAccessible(true);
            int pkey = (Integer) getPkeyContext.invoke(opensslkey);
            getPkeyContext.setAccessible(false);

            // 112 with TLS 1.2 (172 back with 4.3), 36 with TLS 1.0
            byte[] signed_bytes = NativeUtils.rsasign(data, pkey);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchMethodException e) {
            err = e;
        } catch (IllegalArgumentException e) {
            err = e;
        } catch (IllegalAccessException e) {
            err = e;
        } catch (InvocationTargetException e) {
            err = e;
        } catch (InvalidKeyException e) {
            err = e;
        }
        VpnStatus.logError(R.string.error_rsa_sign, err.getClass().toString(), err.getLocalizedMessage());

        return null;

    }


}