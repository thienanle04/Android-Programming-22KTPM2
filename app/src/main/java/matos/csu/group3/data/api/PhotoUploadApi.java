package matos.csu.group3.data.api;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PhotoUploadApi {
    @Multipart
    @POST("/upload-photo")
    Call<List<String>> uploadPhoto(
            @Part MultipartBody.Part image
    );
}
