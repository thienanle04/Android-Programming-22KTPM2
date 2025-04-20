package matos.csu.group3.service;

import matos.csu.group3.data.api.AuthRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("api/auth/token")
    Call<String> getToken(@Body AuthRequest authRequest);
}