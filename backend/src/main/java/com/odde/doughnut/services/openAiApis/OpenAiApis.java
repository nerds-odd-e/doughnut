package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiApis {
  private OpenAiService service;
  private OpenAiApi openAiApi;

  public OpenAiApis(OpenAiService service, OpenAiApi openAiApi) {
    this.service = service;
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
      throw e;
    }
  }

  public String getOpenAiCompletion(String prompt) {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt(prompt)
            .model("text-davinci-003")
            // This can go higher (up to 4000 - prompt size), but openAI performance goes down
            // https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
            .maxTokens(150)
            .build();
    List<CompletionChoice> choices = getCompletionChoices(completionRequest);
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining("")).trim();
  }
}
