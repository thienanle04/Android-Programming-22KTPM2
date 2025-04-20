package matos.csu.group3.service;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String IP = "10.0.2.2";

//    private static final String IP = "192.168.0.102";
    private static String BASE_URL = "";

    private static Retrofit retrofit;
    private static String jwtToken = "";

    private static final int PORT = 8080;

    // Method to initialize the API client with discovered server details
    public static void initialize(String token) {
        BASE_URL = "https://" + IP + ":" + PORT + "/";
        jwtToken = "Bearer " + token;
        retrofit = null; // Reset retrofit to force recreation with new base URL
    }

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            if (BASE_URL.isEmpty() || jwtToken.isEmpty()) {
                throw new IllegalStateException("API Client not initialized. Call initialize() first.");
            }

            Gson gson = new GsonBuilder()
                    .setLenient() // This allows parsing of malformed JSON
                    .create();

            OkHttpClient client = getUnsafeOkHttpClientBuilder()
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
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    // Method to get the auth service for token retrieval
    public static AuthService getAuthService() {
        String tempBaseUrl = "https://" + IP + ":" + PORT + "/";

        Gson gson = new GsonBuilder()
                .setLenient() // This allows parsing of malformed JSON
                .create();

        OkHttpClient tempClient = getUnsafeOkHttpClientBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit tempRetrofit = new Retrofit.Builder()
                .baseUrl(tempBaseUrl)
                .client(tempClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return tempRetrofit.create(AuthService.class);
    }

    private static OkHttpClient.Builder getUnsafeOkHttpClientBuilder() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJwtToken() {
        return jwtToken;
    }
}