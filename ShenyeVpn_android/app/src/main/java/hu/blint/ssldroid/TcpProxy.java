package hu.blint.ssldroid;

import java.io.IOException;
import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
public class TcpProxy {
    private String tunnelName;
    private int listenPort;
    private String tunnelHost;
    private int tunnelPort;
    private String p12file, p12pass;
    private TcpProxyServerThread server = null;
    private boolean is_last;

    public TcpProxy(String tunnelName, int listenPort, String targetHost, int targetPort, String p12file, String p12pass, boolean is_last) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = targetHost;
        this.tunnelPort = targetPort;
        this.p12file = p12file;
        this.p12pass = p12pass;
        this.is_last = is_last;
    }

    public void serve() throws IOException {
        server = new TcpProxyServerThread(this.tunnelName, this.listenPort, this.tunnelHost,
                                          this.tunnelPort, this.p12file, this.p12pass, is_last);
        server.start();
    }

    public void stop() {
        if (server != null) {
            try {
                //close the server socket and interrupt the server thread
                server.ss.close();
                Thread tmpThread = server;
                server = null;
                if (tmpThread!=null)
                    tmpThread.interrupt();
            } catch (Exception e) {
                Log.d("SSLDroid", "Interrupt failure: " + e.toString());
            }
        }
        Log.d("SSLDroid", "Stopping tunnel "+this.listenPort+":"+this.tunnelHost+":"+this.tunnelPort);
    }

    //if the listening socket is still active, we're alive
    public boolean isAlive() {
        return server.ss.isBound();
    }

}
