package com.odde.doughnut.services.openAiApis;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.runs.Run;
import com.theokanning.openai.runs.RunCreateRequest;
import com.theokanning.openai.threads.Thread;
import com.theokanning.openai.threads.ThreadRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.http.HttpStatus;

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

  public Optional<JsonNode> getFunctionCallArguments(ChatCompletionRequest chatRequest) {
    return getFunctionCall(chatRequest).map(ChatFunctionCall::getArguments);
  }

  public Optional<ChatFunctionCall> getFunctionCall(ChatCompletionRequest chatRequest) {
    return chatCompletion(chatRequest)
        //        .map(x->{
        //          System.out.println(chatRequest);
        //          System.out.println(x);
        //          return x;
        //        })
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getFunctionCall);
  }

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    return blockGet(openAiApi.createChatCompletion(request)).getChoices().stream().findFirst();
  }

  public List<Model> getModels() {
    return blockGet(openAiApi.listModels()).data;
  }

  public String uploadFineTuningExamples(
      List<OpenAIChatGPTFineTuningExample> examples, String subFileName) throws IOException {
    FineTuningFileWrapper uploader = new FineTuningFileWrapper(examples, subFileName);
    return uploader.withFileToBeUploaded(
        (file) -> {
          RequestBody purpose = RequestBody.create("fine-tune", MediaType.parse("text/plain"));
          try {
            return blockGet(openAiApi.uploadFile(purpose, file)).getId();
          } catch (Exception e) {
            throw new OpenAIServiceErrorException(
                "Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
          }
        });
  }

  public FineTuningJob triggerFineTuning(String fileId) {
    FineTuningJobRequest fineTuningJobRequest = new FineTuningJobRequest();
    fineTuningJobRequest.setTrainingFile(fileId);
    fineTuningJobRequest.setModel("gpt-3.5-turbo-1106");
    fineTuningJobRequest.setHyperparameters(
        new Hyperparameters(3)); // not sure what should be the nEpochs value

    FineTuningJob fineTuningJob = blockGet(openAiApi.createFineTuningJob(fineTuningJobRequest));
    if (List.of("failed", "cancelled").contains(fineTuningJob.getStatus())) {
      throw new OpenAIServiceErrorException(
          "Trigger Fine-Tuning Failed: " + fineTuningJob, HttpStatus.BAD_REQUEST);
    }
    return fineTuningJob;
  }

  public Assistant createAssistant(AssistantRequest assistantRequest) {
    return openAiApi.createAssistant(assistantRequest).blockingGet();
  }

  public Thread createThread(ThreadRequest threadRequest) {
    return openAiApi.createThread(threadRequest).blockingGet();
  }

  public Run createRun(String threadId, RunCreateRequest runCreateRequest) {
    return openAiApi.createRun(threadId, runCreateRequest).blockingGet();
  }
}
