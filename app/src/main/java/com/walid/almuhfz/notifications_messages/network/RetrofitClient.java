package com.walid.almuhfz.notifications_messages.network;

import com.walid.almuhfz.network.NetworkClient;

import retrofit2.Retrofit;

public class RetrofitClient {
    private static final String BASE_URL = "https://post.walid-fekry.com/";
    private static volatile Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    retrofit = NetworkClient.getInstance().createRetrofit(BASE_URL);
                }
            }
        }
        return retrofit;
    }
}
