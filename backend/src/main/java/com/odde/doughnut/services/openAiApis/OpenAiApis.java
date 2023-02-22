package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiApis {
  private OpenAiApi openAiApi;

  public OpenAiApis(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public static OpenAiApi getOpenAiApi(String openAiToken, String baseUrl) {
    ObjectMapper mapper = defaultObjectMapper();
    OkHttpClient client = defaultClient(openAiToken, Duration.ofSeconds(60));
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build();

    return retrofit.create(OpenAiApi.class);
  }

  private List<CompletionChoice> getCompletionChoices(CompletionRequest completionRequest) {
    try {
      return openAiApi.createCompletion(completionRequest).blockingGet().getChoices();
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      if (5 == e.code() / 100) {
        throw new OpenAIServiceErrorException(e.getMessage(), HttpStatus.valueOf(e.code()));
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

  public AiSuggestion getOpenAiCompletion(String prompt) {
    CompletionRequest completionRequest = getCompletionRequest(prompt);
    List<CompletionChoice> choices = getCompletionChoices(completionRequest);
    return new AiSuggestion(choices.stream().findFirst().orElse(null));
  }

  private static CompletionRequest getCompletionRequest(String prompt) {
    return CompletionRequest.builder()
        .prompt(prompt)
        .model("text-davinci-003")
        // This can go higher (up to 4000 - prompt size), but openAI performance goes down
        // https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
        .maxTokens(50)
        //
        // an effort has been made the response more responsive by using stream(true)
        // how every, due to the library limitation, we cannot do it yet.
        // find more details here:
        //     https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .echo(true)
        .build();
  }

  private CreateImageRequest getImageRequest(String prompt) {
    return CreateImageRequest.builder().prompt(prompt).responseFormat("b64_json").build();
  }

  public String getOpenAiImage(String prompt) {
    CreateImageRequest completionRequest = getImageRequest(prompt);
    ImageResult choices = getAImage(completionRequest);
    return choices.getData().get(0).getB64Json();
  }

  private ImageResult getAImage(CreateImageRequest completionRequest) {
    try {
      return openAiApi.createImage(completionRequest).blockingGet();
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      if (5 == e.code() / 100) {
        throw new OpenAIServiceErrorException(e.getMessage(), HttpStatus.valueOf(e.code()));
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
