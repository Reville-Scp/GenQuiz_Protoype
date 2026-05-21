package com.example.genquizlayout;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    // Sends the selected PDF to Spring Boot and receives the generated quiz text.
    @Multipart
    @POST("api/generate-quiz")
    Call<ResponseBody> generateQuiz(
            @Part MultipartBody.Part file
    );

    // Sends a Word/Excel/PowerPoint file to Spring Boot and receives a converted PDF.
    @Multipart
    @POST("api/convert-pdf")
    Call<ResponseBody> convertToPdf(
            @Part MultipartBody.Part file
    );
}
