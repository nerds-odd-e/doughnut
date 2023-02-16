package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
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
      throw e;
    }
  }

  public Flux<String> getOpenAiCompletion(String prompt) {
    Optional<CompletionChoice> first = getCompletionChoice(prompt, 4);
    String completedRaw = first.map(CompletionChoice::getText).orElse("").trim();
    return Flux.just(completedRaw);
  }

  @NotNull
  private Optional<CompletionChoice> getCompletionChoice(String prompt, int retriesLeft) {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
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
    List<CompletionChoice> choices = getCompletionChoices(completionRequest);

    Optional<CompletionChoice> first = choices.stream().findFirst();
    return first.flatMap(
        choice ->
            "length".equals(choice.getFinish_reason()) && retriesLeft > 0
                ? getCompletionChoice(choice.getText(), retriesLeft - 1)
                : first);
  }
}
