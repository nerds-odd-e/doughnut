package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ai.client.OpenAiApi2;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.service.ChatCompletionRequestMixIn;
import com.theokanning.openai.service.ChatFunctionCallMixIn;
import com.theokanning.openai.service.ChatFunctionMixIn;
import io.reactivex.Single;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpStatus;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public record ApiExecutor() {
  public static OpenAiApi2 getOpenAiApi(String openAiToken, String baseUrl) {
    ObjectMapper mapper = defaultObjectMapper();
    OkHttpClient client = defaultClient(openAiToken, Duration.ofSeconds(60));
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    return retrofit.create(OpenAiApi2.class);
  }

  private static ObjectMapper defaultObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.addMixIn(ChatFunction.class, ChatFunctionMixIn.class);
    mapper.addMixIn(ChatCompletionRequest.class, ChatCompletionRequestMixIn.class);
    mapper.addMixIn(ChatFunctionCall.class, ChatFunctionCallMixIn.class);
    return mapper;
  }

  public static OkHttpClient defaultClient(String token, Duration timeout) {
    return (new OkHttpClient.Builder())
        .addInterceptor(new AuthenticationInterceptor(token))
        .connectionPool(new ConnectionPool(5, 1L, TimeUnit.SECONDS))
        .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
        .build();
  }

  public static <T> T blockGet(Single<T> apply) {
    try {
      return execute(apply);
    } catch (OpenAiHttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.statusCode) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      if (e.statusCode / 100 == 5) {
        throw new OpenAIServiceErrorException(e.getMessage(), HttpStatus.valueOf(e.statusCode));
      }
      throw e;
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SocketTimeoutException) {
        throw new OpenAITimeoutException(cause.getMessage());
      }
      throw e;
    }
  }
}
