package com.example.genquizlayout;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {

        if (retrofit == null) {

            // AI quiz generation can take a while, so the app waits longer before timing out.
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();

            // 10.0.2.2 means the Android emulator will connect to localhost on your laptop.
            retrofit = new Retrofit.Builder()
//                    .baseUrl("http://10.0.2.2:8080/")
                    .baseUrl("https://backendgenquiz.onrender.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
