package com.walid.almuhfz.learning.networking;

import com.walid.almuhfz.learning.models.ReciterResponse;
import com.walid.almuhfz.learning.models.SoraDetailsResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface Api {

    @GET("v1/edition?language=ar")
    Call<ReciterResponse> getListOfReciter();

    @GET("v1/surah/{soraId}/{identifier}")
    Call<SoraDetailsResponse> getSoraDetails(@Path(value = "soraId", encoded = true) Integer soraId,
                                             @Path(value = "identifier", encoded = true) String identifier);

    @GET
    @Streaming
    Call<ResponseBody> downloadFileWithDynamicUrl(@Url String fileUrl);

    @GET
    @Streaming
    Call<ResponseBody> downloadFileWithRange(@Url String fileUrl, @Header("Range") String rangeHeader);
}
