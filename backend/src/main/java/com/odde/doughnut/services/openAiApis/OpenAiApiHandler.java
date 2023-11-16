package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.OpenAITimeoutException;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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
        .map(
            (x) -> {
              System.out.println(x);
              return x;
            })
        .map(ChatMessage::getFunctionCall)
        .map(ChatFunctionCall::getArguments);
  }

  public Optional<AiCompletion> getAiCompletion(
      AiCompletionParams aiCompletionParams, ChatCompletionRequest chatCompletionRequest) {
    return chatCompletion(chatCompletionRequest).map(aiCompletionParams::getAiCompletion);
  }

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return withExceptionHandler(
        () ->
            openAiApi.createChatCompletion(request).blockingGet().getChoices().stream()
                .findFirst());
  }

  public FineTuningJob triggerFineTune(FineTuningJobRequest fineTuningJobRequest) {
    return withExceptionHandler(
        () -> {
          FineTuningJob fineTuningJob =
              openAiApi.createFineTuningJob(fineTuningJobRequest).blockingGet();
          List<String> failed = List.of("failed", "cancelled");
          if (failed.contains(fineTuningJob.getStatus())) {
            throw new OpenAIServiceErrorException(
                "Trigger Failed: " + defaultObjectMapper().writeValueAsString(fineTuningJob),
                HttpStatus.valueOf(500));
          }
          return fineTuningJob;
        });
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

  public List<Model> getModels() {
    return openAiApi.listModels().blockingGet().data;
  }

  public String uploadFineTuningExamples(List<? extends Object> examples, String subFileName)
      throws IOException {
    FineTuningFileWrapper uploader = new FineTuningFileWrapper(examples, subFileName);
    return uploader.withFileToBeUploaded(
        (file) -> {
          RequestBody purpose = RequestBody.create("fine-tune", MediaType.parse("text/plain"));
          try {
            return execute(openAiApi.uploadFile(purpose, file)).getId();
          } catch (Exception e) {
            throw new OpenAIServiceErrorException(
                "Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
          }
        });
  }
}
