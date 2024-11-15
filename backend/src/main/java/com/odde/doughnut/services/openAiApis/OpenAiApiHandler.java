package com.odde.doughnut.services.openAiApis;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;
import static java.lang.Thread.sleep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolList;
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

  public Optional<JsonNode> getFirstToolCallArguments(ChatCompletionRequest chatRequest) {
    return getFirstToolCalls(chatRequest)
        .map(ChatToolCall::getFunction)
        .map(ChatFunctionCall::getArguments);
  }

  private Optional<ChatToolCall> getFirstToolCalls(ChatCompletionRequest chatRequest) {
    return chatCompletion(chatRequest)
        //        .map(x->{
        //          System.out.println(chatRequest);
        //          System.out.println(x);
        //          return x;
        //        })
        .map(ChatCompletionChoice::getMessage)
        .map(AssistantMessage::getToolCalls)
        .flatMap(x -> x.stream().findFirst());
  }

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return blockGet(openAiApi.createChatCompletion(request)).getChoices().stream().findFirst();
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
          RequestBody.create(tempFile, MediaType.parse("application/octet-stream"));
      MultipartBody.Part filePart =
          MultipartBody.Part.createFormData("file", tempFile.getName(), fileRequestBody);
      RequestBody purposeBody = RequestBody.create(purpose, MediaType.parse("text/plain"));
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
    while (retrievedRun.getStatus() == null
        || !(retrievedRun.getStatus().equals("completed"))
            && !(retrievedRun.getStatus().equals("failed"))
            && !(retrievedRun.getStatus().equals("requires_action"))) {
      count++;
      if (count > 15) {
        break;
      }
      wait(count - 1);
      retrievedRun = retrieveRun(threadId, currentRun.getId());
    }
    if (retrievedRun.getStatus().equals("requires_action")
        || retrievedRun.getStatus().equals("completed")) {
      return retrievedRun;
    }
    throw new RuntimeException("OpenAI run status: " + retrievedRun.getStatus());
  }

  private static void wait(int hundredMilliSeconds) {
    try {
      sleep(hundredMilliSeconds * 200L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public Run submitToolOutputs(String threadId, String runId, String toolCallId, Object result)
      throws JsonProcessingException {
    SubmitToolOutputRequestItem toolOutputRequestItem =
        SubmitToolOutputRequestItem.builder()
            .toolCallId(toolCallId)
            .output(new ObjectMapper().writeValueAsString(result))
            .build();
    List<SubmitToolOutputRequestItem> toolOutputRequestItems = new ArrayList<>();
    toolOutputRequestItems.add(toolOutputRequestItem);
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

  public Optional<JsonNode> requestAndGetFunctionCallArguments(
      AiToolList tool, OpenAIChatRequestBuilder chatAboutNoteRequestBuilder1) {
    ChatCompletionRequest chatRequest = chatAboutNoteRequestBuilder1.addTool(tool).build();
    return getFirstToolCallArguments(chatRequest);
  }

  public Run cancelRun(String threadId, String runId) {
    return openAiApi.cancelRun(threadId, runId).blockingGet();
  }
}
