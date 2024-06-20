package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class AiAdvisorService {

  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
  }

  public AiAssistantResponse initiateAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String assistantId) {
    return getContentCompletionService()
        .initiateAThread(note, assistantId, aiCompletionParams.getCompletionPrompt());
  }

  public AiAssistantResponse answerAiCompletionClarifyingQuestion(
      AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    return getContentCompletionService()
        .answerAiCompletionClarifyingQuestion(answerClarifyingQuestionParams);
  }

  public Assistant createNoteAssistant(String modelName) {
    return getContentCompletionService().createNoteAssistant(modelName);
  }

  public String chatWithAi(Note note, String userMessage, String assistantId) {
    return getContentCompletionService()
        .initiateAThread(note, assistantId, userMessage)
        .getLastMessage();
  }

  public List<String> getAvailableGptModels() {
    List<String> modelVersionOptions = new ArrayList<>();

    openAiApiHandler
        .getModels()
        .forEach(
            (e) -> {
              if (e.id.startsWith("ft:") || e.id.startsWith("gpt")) {
                modelVersionOptions.add(e.id);
              }
            });

    return modelVersionOptions;
  }

  public String uploadAndTriggerFineTuning(
      List<OpenAIChatGPTFineTuningExample> examples, String question) throws IOException {
    String fileId = openAiApiHandler.uploadFineTuningExamples(examples, question);
    return openAiApiHandler.triggerFineTuning(fileId).getFineTunedModel();
  }

  private ContentCompletionService getContentCompletionService() {
    return new ContentCompletionService(openAiApiHandler);
  }

  public SrtDto getTranscription(String filename, byte[] bytes) throws IOException {
    RequestBody requestFile = RequestBody.create(bytes, MediaType.parse("multipart/form-data"));

    MultipartBody.Part body = MultipartBody.Part.createFormData("file", filename, requestFile);

    MultipartBody.Builder builder =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(body)
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("response_format", "srt");

    RequestBody requestBody = builder.build();
    return openAiApiHandler.getTranscription(requestBody);
  }

  public Map<String, String> createAllDefaultAssistants(
      Timestamp currentUTCTimestamp, GlobalSettingsService globalSettingsService) {
    Map<String, String> result = new HashMap<>();
    result.put(
        "note details completion",
        createCompletionAssistant(currentUTCTimestamp, globalSettingsService));
    result.put("chat", createChatAssistant(currentUTCTimestamp, globalSettingsService));

    return result;
  }

  private String createCompletionAssistant(
      Timestamp currentUTCTimestamp, GlobalSettingsService globalSettingsService) {
    Assistant noteCompletionAssistant =
        createNoteAssistant(globalSettingsService.globalSettingOthers().getValue());
    String id = noteCompletionAssistant.getId();
    globalSettingsService.noteCompletionAssistantId().setKeyValue(currentUTCTimestamp, id);
    return id;
  }

  private String createChatAssistant(
      Timestamp currentUTCTimestamp, GlobalSettingsService globalSettingsService) {
    Assistant chatAssistant =
        createNoteAssistant(globalSettingsService.globalSettingOthers().getValue());
    String id = chatAssistant.getId();
    globalSettingsService.noteCompletionAssistantId().setKeyValue(currentUTCTimestamp, id);
    return id;
  }
}
