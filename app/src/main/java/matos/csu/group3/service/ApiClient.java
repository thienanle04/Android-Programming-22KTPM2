package matos.csu.group3.service;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.0.105:8080/";
    private static Retrofit retrofit;
    private static final String jwtToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjbGllbnQiLCJpYXQiOjE3NDUwODExNzcsImV4cCI6MTc0NTA4NDc3N30.__-zEuakrN_KUtPQXyeuWXeNYDZtcO5BER5qYqQD0HOpCs_kiI49_S0UrNeNc1E4KkDIHVeeGELYYhdKT-W_Yw";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Authorization", jwtToken)
                                    .method(original.method(), original.body());

                            Request request = requestBuilder.build();
                            return chain.proceed(request);
                        }
                    })
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
