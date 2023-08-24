package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiApiHandler {
  protected final OpenAiApi openAiApi;

  public OpenAiApiHandler(OpenAiApi openAiApi) {
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

  public String getOpenAiImage(String prompt) {
    return withExceptionHandler(
        () -> {
          CreateImageRequest completionRequest =
              CreateImageRequest.builder().prompt(prompt).responseFormat("b64_json").build();
          ImageResult choices = openAiApi.createImage(completionRequest).blockingGet();
          return choices.getData().get(0).getB64Json();
        });
  }

  public Optional<JsonNode> getFunctionCallArguments(ChatCompletionRequest chatRequest) {
    return chatCompletion(chatRequest)
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getFunctionCall)
        .map(ChatFunctionCall::getArguments);
  }

  public Optional<AiCompletion> getAiCompletion(
      AiCompletionRequest aiCompletionRequest, ChatCompletionRequest chatCompletionRequest) {
    return chatCompletion(chatCompletionRequest).map(aiCompletionRequest::getAiCompletion);
  }

  private Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst());
  }

  private <T> T withExceptionHandler(Callable<T> callable) {
    try {
      return callable.call();
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      if (e.code() / 100 == 5) {
        throw new OpenAIServiceErrorException(e.getMessage(), HttpStatus.valueOf(e.code()));
      }
      System.out.println(e.message());
      throw e;
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      if (cause instanceof SocketTimeoutException) {
        throw new OpenAITimeoutException(cause.getMessage());
      }
      throw e;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public String getOpenAiAnswer(String askStatement) {
    List messages = new ArrayList<ChatMessage>();
    ChatMessage message1 = new ChatMessage(ChatMessageRole.USER.value(), "");
    ChatMessage message2 = new ChatMessage(ChatMessageRole.ASSISTANT.value(), askStatement);
    messages.add(message1);
    messages.add(message2);

    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model("gpt-4").messages(messages).stream(false)
            .n(1)
            .maxTokens(100)
            .build();

    Optional<ChatCompletionChoice> result =
        openAiApi.createChatCompletion(request).blockingGet().getChoices().stream().findFirst();
    return result.get().getMessage().getContent();
  }
}
