package shenye.vpn.android.network;

import android.util.Base64;

import retrofit.client.Response;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import shenye.vpn.android.BuildConfig;
import shenye.vpn.android.network.responses.ServersResponse;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.GET;

public class VPNService {
    private static final String API_URL = "https://www.shenyeymz.com/shenye";
    private Client mClient;

    private VPNService(String username, String password) {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder().setEndpoint(API_URL+"/Android");

        if (username != null && password != null) {
            password = this.MD5(password);
            final String credentials = username + ":" + password + ":3:" + BuildConfig.VERSION_NAME;
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

        @Multipart
        @POST("/update_session")
        void update_session(@Part("userid") String userid, @Part("local_ip") String local_ip, @Part("public_ip") String public_ip, @Part("location") String location, @Part("os") String os, @Part("server") int server, Callback<Response> cb);
    }
}
