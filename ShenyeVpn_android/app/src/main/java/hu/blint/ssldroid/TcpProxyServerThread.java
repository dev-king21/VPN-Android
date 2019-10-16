package hu.blint.ssldroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.io.File;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class TcpProxyServerThread extends Thread {

    String tunnelName;
    private int listenPort;
    private String tunnelHost;
    private int tunnelPort;
    private String keyFile;
    ServerSocket ss = null;
    private int session_id = 0;
    private SSLSocketFactory sslSocketFactory;
    private boolean is_last = false;
    private String static_pass = "ghostman123!@#";

    public TcpProxyServerThread(String tunnelName, int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass, boolean is_last) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = tunnelHost;
        this.tunnelPort = tunnelPort;
        this.keyFile = keyFile;
        this.is_last = is_last;

        if (!(new File(keyFile)).exists()) {
            try {

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, keyPass.toCharArray());
                FileOutputStream fos = new FileOutputStream(keyFile);
                keyStore.store(fos, MD5(static_pass).toCharArray());

            } catch (FileNotFoundException e) {
                Log.d("SSLDroid", tunnelName + "/" + session_id + ": Error loading the client certificate file:"
                        + e.toString());
            } catch (NoSuchAlgorithmException e) {
                Log.d("SSLDroid", tunnelName + "/" + session_id + ": No common SSL algorithm found: " + e.toString());
            } catch (KeyStoreException e) {
                Log.d("SSLDroid", tunnelName + "/" + session_id + ": Error setting up keystore:" + e.toString());
            } catch (java.security.cert.CertificateException e) {
                Log.d("SSLDroid", tunnelName + "/" + session_id + ": Error loading the client certificate:" + e.toString());
            } catch (IOException e) {
                Log.d("SSLDroid", tunnelName + "/" + session_id + ": Error loading the client certificate file:" + e.toString());
            }
        }
    }

    // Create a trust manager that does not validate certificate chains
    // TODO: handle this somehow properly (popup if cert is untrusted?)
    // TODO: cacert + crl should be configurable
    TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    public final SSLSocketFactory getSocketFactory(String pkcsFile) {
        if (sslSocketFactory == null) {
            try {
                KeyManagerFactory keyManagerFactory;
                if (pkcsFile != null && !pkcsFile.isEmpty()) {
                    keyManagerFactory = KeyManagerFactory.getInstance("X509");
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new FileInputStream(pkcsFile), MD5(static_pass).toCharArray());
                    keyManagerFactory.init(keyStore, MD5(static_pass).toCharArray());
                } else {
                    keyManagerFactory = null;
                }
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustAllCerts,
                        new SecureRandom());
                sslSocketFactory = context.getSocketFactory();
            } catch (FileNotFoundException e) {
                Log.d("SSLDroid", tunnelName+": Error loading the client certificate file:"
                        + e.toString());
            } catch (KeyManagementException e) {
                Log.d("SSLDroid", tunnelName+": No SSL algorithm support: " + e.toString());
            } catch (NoSuchAlgorithmException e) {
                Log.d("SSLDroid", tunnelName+": No common SSL algorithm found: " + e.toString());
            } catch (KeyStoreException e) {
                Log.d("SSLDroid", tunnelName+": Error setting up keystore:" + e.toString());
            } catch (java.security.cert.CertificateException e) {
                Log.d("SSLDroid", tunnelName+": Error loading the client certificate:" + e.toString());
            } catch (IOException e) {
                Log.d("SSLDroid", tunnelName+": Error loading the client certificate file:" + e.toString());
            } catch (UnrecoverableKeyException e) {
                Log.d("SSLDroid", tunnelName+": Error loading the client certificate:" + e.toString());
            }
        }
        return sslSocketFactory;
    }

    public void run() {
        try {
            ss = new ServerSocket(listenPort, 50, InetAddress.getLocalHost());
            Log.d("SSLDroid", "Listening for connections on "+InetAddress.getLocalHost().getHostAddress()+":"+
                    + this.listenPort + " ...");
        } catch (Exception e) {
            Log.d("SSLDroid", "Error setting up listening socket: " + e.toString());
            return;
        }

        while (true) {
            try {
                Thread fromBrowserToServer = null;
                Thread fromServerToBrowser = null;

                if (isInterrupted()) {
                    Log.d("SSLDroid", tunnelName+": Interrupted server thread, closing sockets...");
                    ss.close();
                    return;
                }
                // accept the connection from my client
                Socket sc = null;
                try {
                    if (is_last)
                        TcpProxyServerService.TUNNEL_CREATED = true;
                    sc = ss.accept();
                    session_id++;
                } catch (SocketException e) {
                    //Log.d("SSLDroid", "Accept failure: " + e.toString());
                }
                Socket st = null;
                try {
                    final SSLSocketFactory sf = getSocketFactory(this.keyFile);
                    st = (SSLSocket) sf.createSocket(this.tunnelHost, this.tunnelPort);
                    setSNIHost(sf, (SSLSocket) st, this.tunnelHost);
                    ((SSLSocket) st).startHandshake();

                } catch (IOException e) {
                    Log.d("SSLDroid", tunnelName+": SSL failure: " + e.toString());
                    return;
                } catch (Exception e) {
                    Log.d("SSLDroid", tunnelName+": SSL failure: " + e.toString());
                }

                if (sc == null || st == null) {
                    Log.d("SSLDroid", tunnelName+"/"+ session_id +": Trying socket operation on a null socket, returning");
                    return;
                }
                
                // relay the stuff through
                fromBrowserToServer = new Relay(
                        this, sc.getInputStream(), st.getOutputStream(), "client", session_id);
                fromServerToBrowser = new Relay(
                        this, st.getInputStream(), sc.getOutputStream(), "server", session_id);

                fromBrowserToServer.start();
                fromServerToBrowser.start();

            } catch (IOException ee) {
                Log.d("SSLDroid", tunnelName+"/"+ session_id +": Ouch: " + ee.toString());
            }
        }
    }

    private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ((android.net.SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
        } else {
            try {
                socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
            } catch (Throwable e) {
                // ignore any error, we just can't set the hostname...
                e.printStackTrace();
            }
        }
    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
};

