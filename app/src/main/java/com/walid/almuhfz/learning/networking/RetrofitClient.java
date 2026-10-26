package com.walid.almuhfz.learning.networking;

import com.walid.almuhfz.network.NetworkClient;

import retrofit2.Retrofit;

public class RetrofitClient {
    public static final String BASE_URL = "https://api.alquran.cloud/";
    private static volatile RetrofitClient mInstance;
    private final Api api;

    private RetrofitClient() {
        Retrofit retrofit = NetworkClient.getInstance().createRetrofit(BASE_URL);
        api = retrofit.create(Api.class);
    }

    public static RetrofitClient getInstance() {
        if (mInstance == null) {
            synchronized (RetrofitClient.class) {
                if (mInstance == null) {
                    mInstance = new RetrofitClient();
                }
            }
        }
        return mInstance;
    }

    public Api getApi() {
        return api;
    }
}
