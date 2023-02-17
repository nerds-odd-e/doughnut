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
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
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
    return getCompletionChoice(prompt, 5).map(CompletionChoice::getText);
  }

  private Flux<CompletionChoice> getCompletionChoice(String prompt, final int retriesLeft) {
    AtomicInteger count = new AtomicInteger();
    return Flux.deferContextual(
        contextView -> {
          return Flux.generate(
              () -> prompt,
              (pmt, sink) -> {
                if (count.getAndIncrement() < retriesLeft) {
                  Optional<CompletionChoice> first = getCompletionChoice1(pmt);
                  if (first.isPresent()) {
                    CompletionChoice choice = first.get();
                    sink.next(choice);
                    if ("length".equals(choice.getFinish_reason())) {
                      return choice.getText();
                    }
                  }
                }
                sink.complete();
                return null;
              });
        });
  }

  private Optional<CompletionChoice> getCompletionChoice1(String prompt) {
    CompletionRequest completionRequest = getCompletionRequest(prompt);
    List<CompletionChoice> choices = getCompletionChoices(completionRequest);

    Optional<CompletionChoice> first = choices.stream().findFirst();
    return first;
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
}
