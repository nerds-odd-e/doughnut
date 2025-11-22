package com.odde.doughnut.services.openAiApis;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Handler for OpenAI API calls, supporting Chat Completion API.
 *
 * <p>Chat Completion API methods: {@link #chatCompletion}, {@link #streamChatCompletion}
 */
@Service
public class OpenAiApiHandler {
  private final OpenAiApi openAiApi;

  @Autowired
  public OpenAiApiHandler(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public String getOpenAiImage(String prompt) {
    CreateImageRequest completionRequest =
        CreateImageRequest.builder().prompt(prompt).responseFormat("b64_json").build();
    ImageResult choices = blockGet(openAiApi.createImage(completionRequest));

    return choices.getData().get(0).getB64Json();
  }

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    ChatCompletionResult result = blockGet(openAiApi.createChatCompletion(request));
    if (result == null || result.getChoices() == null || result.getChoices().isEmpty()) {
      return Optional.empty();
    }
    return result.getChoices().stream().findFirst();
  }

  public Flowable<String> streamChatCompletion(ChatCompletionRequest request) {
    // Rebuild the request with streaming enabled
    ChatCompletionRequest streamingRequest =
        ChatCompletionRequest.builder()
            .model(request.getModel())
            .messages(request.getMessages())
            .stream(true)
            .tools(request.getTools())
            .build();

    return Flowable.create(
        emitter ->
            openAiApi
                .createChatCompletionStream(streamingRequest)
                .enqueue(new ChatCompletionResponseBodyCallback(emitter)),
        BackpressureStrategy.BUFFER);
  }

  public List<Model> getModels() {
    return blockGet(openAiApi.listModels()).data;
  }

  public String uploadTextFile(String subFileName, String content, String purpose, String suffix)
      throws IOException {
    File tempFile = File.createTempFile(subFileName, suffix);
    try {
      Files.write(tempFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);
      RequestBody fileRequestBody =
          RequestBody.create(MediaType.parse("application/octet-stream"), tempFile);
      MultipartBody.Part filePart =
          MultipartBody.Part.createFormData("file", tempFile.getName(), fileRequestBody);
      RequestBody purposeBody = RequestBody.create(MediaType.parse("text/plain"), purpose);
      try {
        return blockGet(openAiApi.uploadFile(purposeBody, filePart)).getId();
      } catch (Exception e) {
        throw new OpenAIServiceErrorException("Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } finally {
      tempFile.delete();
    }
  }

  public FineTuningJob triggerFineTuning(String fileId) {
    FineTuningJobRequest fineTuningJobRequest = new FineTuningJobRequest();
    fineTuningJobRequest.setTrainingFile(fileId);
    fineTuningJobRequest.setModel("gpt-3.5-turbo-1106");
    fineTuningJobRequest.setHyperparameters(
        new Hyperparameters()); // not sure what should be the nEpochs value

    FineTuningJob fineTuningJob = blockGet(openAiApi.createFineTuningJob(fineTuningJobRequest));
    if (List.of("failed", "cancelled").contains(fineTuningJob.getStatus())) {
      throw new OpenAIServiceErrorException(
          "Trigger Fine-Tuning Failed: " + fineTuningJob, HttpStatus.BAD_REQUEST);
    }
    return fineTuningJob;
  }

  public String getTranscription(RequestBody requestBody) throws IOException {
    return blockGet(((OpenAiApiExtended) openAiApi).createTranscriptionSrt(requestBody)).string();
  }

  public Optional<JsonNode> requestAndGetJsonSchemaResult(
      InstructionAndSchema tool, OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    ChatCompletionRequest chatRequest = openAIChatRequestBuilder.responseJsonSchema(tool).build();

    try {
      return chatCompletion(chatRequest)
          .map(ChatCompletionChoice::getMessage)
          .map(AssistantMessage::getContent)
          .map(
              content -> {
                try {
                  return new ObjectMapperConfig().objectMapper().readTree(content);
                } catch (JsonProcessingException e) {
                  return null;
                }
              });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof MismatchedInputException) {
        return Optional.empty();
      }
      throw e;
    }
  }
}
