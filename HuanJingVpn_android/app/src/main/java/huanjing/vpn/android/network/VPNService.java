package huanjing.vpn.android.network;

import android.util.Base64;

import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import huanjing.vpn.android.BuildConfig;
import huanjing.vpn.android.network.responses.ServersResponse;
import huanjing.vpn.android.network.responses.SmartDNSResponse;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.GET;

public class VPNService {
    private static final String API_URL = "https://shenyeymz.com/shenye"; /*10.0.2.2*/
    private Client mClient;

    private VPNService(String username, String password) {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder().setEndpoint(API_URL+"/Android");

        if (username != null && password != null) {
            password = this.MD5(password);
            final String credentials = username + ":" + password + ":" + BuildConfig.VERSION_NAME;
            adapterBuilder.setRequestInterceptor(new UserAgentInterceptor() {
                @Override
                public void intercept(RequestInterceptor.RequestFacade request) {
                    super.intercept(request);
                    String string = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    request.addHeader("Authorization", string);
                }
            });
        }

        RestAdapter restAdapter = adapterBuilder.build();
        mClient = restAdapter.create(Client.class);
    }

    public static VPNService.Client get() {
        return new VPNService(null, null).mClient;
    }

    public static VPNService.Client get(String username, String password) {
        return new VPNService(username, password).mClient;
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

    public interface Client {
        @GET("/server_list")
        void servers(Callback<ServersResponse> callback);
        @GET("/smartdns")
        void smartdns(Callback<SmartDNSResponse> callback);

        @Multipart
        @POST("/update_session")
        void update_session(@Part("userid") String userid, @Part("local_ip") String local_ip, @Part("public_ip") String public_ip, @Part("location") String location, @Part("os") String os, @Part("server") int server, Callback<Response> cb);
    }
}
