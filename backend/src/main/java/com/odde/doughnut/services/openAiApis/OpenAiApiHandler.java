package com.odde.doughnut.services.openAiApis;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;
import static java.lang.Thread.sleep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.assistant.VectorStoreFileRequest;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.run.SubmitToolOutputRequestItem;
import com.theokanning.openai.assistants.run.SubmitToolOutputsRequest;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.assistants.vector_store.VectorStoreRequest;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.assistant_stream.AssistantResponseBodyCallback;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
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
import okhttp3.ResponseBody;
import org.springframework.http.HttpStatus;
import retrofit2.Call;

public class OpenAiApiHandler {
  private final OpenAiApi openAiApi;

  public OpenAiApiHandler(OpenAiApi openAiApi) {
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

  public Flowable<ChatCompletionChunk> streamChatCompletion(ChatCompletionRequest request) {
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

  public Assistant createAssistant(AssistantRequest assistantRequest) {
    return blockGet(openAiApi.createAssistant(assistantRequest));
  }

  public Thread createThread(ThreadRequest threadRequest) {
    return blockGet(openAiApi.createThread(threadRequest));
  }

  public void createMessage(String threadId, MessageRequest messageRequest) {
    blockGet(openAiApi.createMessage(threadId, messageRequest));
  }

  private Run retrieveRun(String threadId, String runId) {
    return blockGet(openAiApi.retrieveRun(threadId, runId));
  }

  public Run createRun(String threadId, RunCreateRequest runCreateRequest) {
    return blockGet(openAiApi.createRun(threadId, runCreateRequest));
  }

  public static Flowable<AssistantSSE> assistantStream(Call<ResponseBody> apiCall) {
    return Flowable.create(
        emitter -> apiCall.enqueue(new AssistantResponseBodyCallback(emitter)),
        BackpressureStrategy.BUFFER);
  }

  public Flowable<AssistantSSE> createRunStream(
      String threadId, RunCreateRequest.RunCreateRequestBuilder runCreateRequestBuilder) {
    RunCreateRequest runCreateRequest = runCreateRequestBuilder.stream(true).build();
    return assistantStream(openAiApi.createRunStream(threadId, runCreateRequest));
  }

  public Run retrieveUntilCompletedOrRequiresAction(String threadId, Run currentRun) {
    Run retrievedRun = currentRun;
    int count = 0;
    while (processingStatus(retrievedRun.getStatus())) {
      count++;
      if (count > 15) {
        break;
      }
      wait(count - 1);
      retrievedRun = retrieveRun(threadId, currentRun.getId());
    }
    return retrievedRun;
  }

  private static boolean processingStatus(String status) {
    return status == null
        || !(status.equals("completed"))
            && !(status.equals("incomplete"))
            && !(status.equals("cancelled"))
            && !(status.equals("failed"))
            && !(status.equals("expired"))
            && !(status.equals("requires_action"));
  }

  private static void wait(int hundredMilliSeconds) {
    try {
      sleep(hundredMilliSeconds * 200L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  public Run submitToolOutputs(String threadId, String runId, String toolCallId, Object result)
      throws JsonProcessingException {
    Map<String, ToolCallResult> results = new HashMap<>();
    results.put(toolCallId, (ToolCallResult) result);
    return submitToolOutputs(threadId, runId, results);
  }

  public Run submitToolOutputs(String threadId, String runId, Map<String, ?> results)
      throws JsonProcessingException {
    List<SubmitToolOutputRequestItem> toolOutputRequestItems = new ArrayList<>();
    for (Map.Entry<String, ?> entry : results.entrySet()) {
      SubmitToolOutputRequestItem build =
          SubmitToolOutputRequestItem.builder()
              .toolCallId(entry.getKey())
              .output(
                  new com.odde.doughnut.configs.ObjectMapperConfig()
                      .objectMapper()
                      .writeValueAsString(entry.getValue()))
              .build();
      toolOutputRequestItems.add(build);
    }

    SubmitToolOutputsRequest submitToolOutputsRequest =
        SubmitToolOutputsRequest.builder().toolOutputs(toolOutputRequestItems).build();
    return blockGet(openAiApi.submitToolOutputs(threadId, runId, submitToolOutputsRequest));
  }

  public List<Message> getThreadMessages(String threadId, String runId) {
    Map<String, Object> options = new HashMap<>();
    options.put("order", "asc");
    if (runId != null) {
      options.put("run_id", runId);
    }
    return blockGet(openAiApi.listMessages(threadId, options)).getData();
  }

  public String getTranscription(RequestBody requestBody) throws IOException {
    return blockGet(((OpenAiApiExtended) openAiApi).createTranscriptionSrt(requestBody)).string();
  }

  public String createVectorFile(String assistantName, String fileId) {
    VectorStoreRequest store = VectorStoreRequest.builder().name(assistantName).build();
    String storeId = blockGet(openAiApi.createVectorStore(store)).getId();
    VectorStoreFileRequest request = VectorStoreFileRequest.builder().fileId(fileId).build();
    blockGet(openAiApi.createVectorStoreFile(storeId, request)).getId();
    return storeId;
  }

  public void cancelRun(String threadId, String runId) {
    openAiApi.cancelRun(threadId, runId).blockingGet();
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
                  return new com.odde.doughnut.configs.ObjectMapperConfig()
                      .objectMapper()
                      .readTree(content);
                } catch (JsonProcessingException e) {
                  return null;
                }
              });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
        return Optional.empty();
      }
      throw e;
    }
  }
}
