package shenye.vpn.android.network;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.http.GET;

public class IPService {
    private static final String API_URL = "http://ip-api.com/";
    private Client mClient;

    private IPService() {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(API_URL).setRequestInterceptor(new UserAgentInterceptor()).build();
        mClient = restAdapter.create(Client.class);
    }

    public static IPService.Client get() {
        return new IPService().mClient;
    }

    public interface Client {
        @GET("/json")
        void status(Callback<Data> callback);
    }

    public class Data {
        public String query;
        public String country;
        public String city;
        public String countryCode;
    }
}
