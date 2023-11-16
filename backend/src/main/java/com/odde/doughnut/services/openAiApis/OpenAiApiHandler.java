package com.odde.doughnut.services.openAiApis;

import static com.theokanning.openai.service.OpenAiService.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.springframework.http.HttpStatus;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class OpenAiApiHandler {
  private final ApiExecutor apiExecutor;

  public OpenAiApiHandler(OpenAiApi openAiApi) {
    this.apiExecutor = new ApiExecutor(openAiApi);
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
    CreateImageRequest completionRequest =
        CreateImageRequest.builder().prompt(prompt).responseFormat("b64_json").build();
    ImageResult choices = apiExecutor.exec((api) -> api.createImage(completionRequest));

    return choices.getData().get(0).getB64Json();
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
    return apiExecutor.exec((api) -> api.createChatCompletion(request)).getChoices().stream()
        .findFirst();
  }

  public List<Model> getModels() {
    return apiExecutor.exec(OpenAiApi::listModels).data;
  }

  public String uploadAndTriggerFineTuning(
      List<OpenAIChatGPTFineTuningExample> questionGenerationTrainingExamples, String Question)
      throws IOException {
    String fileId = uploadFineTuningExamples(questionGenerationTrainingExamples, Question);
    return triggerFineTuning(fileId).getFineTunedModel();
  }

  private String uploadFineTuningExamples(
      List<OpenAIChatGPTFineTuningExample> examples, String subFileName) throws IOException {
    FineTuningFileWrapper uploader = new FineTuningFileWrapper(examples, subFileName);
    return uploader.withFileToBeUploaded(
        (file) -> {
          RequestBody purpose = RequestBody.create("fine-tune", MediaType.parse("text/plain"));
          try {
            return apiExecutor.exec((api) -> api.uploadFile(purpose, file)).getId();
          } catch (Exception e) {
            throw new OpenAIServiceErrorException(
                "Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
          }
        });
  }

  private FineTuningJob triggerFineTuning(String fileId) {
    FineTuningJobRequest fineTuningJobRequest = new FineTuningJobRequest();
    fineTuningJobRequest.setTrainingFile(fileId);
    fineTuningJobRequest.setModel("gpt-3.5-turbo-1106");
    fineTuningJobRequest.setHyperparameters(
        new Hyperparameters(3)); // not sure what should be the nEpochs value

    FineTuningJob fineTuningJob =
        apiExecutor.exec((api) -> api.createFineTuningJob(fineTuningJobRequest));
    if (List.of("failed", "cancelled").contains(fineTuningJob.getStatus())) {
      throw new OpenAIServiceErrorException(
          "Trigger Fine-Tuning Failed: " + fineTuningJob, HttpStatus.BAD_REQUEST);
    }
    return fineTuningJob;
  }
}
