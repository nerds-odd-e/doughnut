package com.odde.doughnut.services.openAiApis;

import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

// The OpenAI API Java project (https://github.com/TheoKanning/openai-java)
// is not actively maintained. The project is a Java client for the OpenAI API.
//
// It uses retrofit2 to make HTTP requests to the OpenAI API.
// We use this extended interface to add more methods to the OpenAI API.
//
// If the OpenAI API Java project is continued to be not actively maintained,
// we can consider forking the project and maintaining it ourselves.
public interface OpenAiApiExtended extends OpenAiApi {
  @POST("/v1/audio/transcriptions")
  @Headers("Accept: text/plain")
  Single<ResponseBody> createTranscriptionSrt(@Body RequestBody var1);
}
