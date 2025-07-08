package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.FineTuningExamples;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.util.*;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public final class OtherAiServices {
  private final OpenAiApiHandler openAiApiHandler;

  public OtherAiServices(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getTimage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
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
    FineTuningExamples fineTuningExamples = new FineTuningExamples(examples);

    String fileId =
        openAiApiHandler.uploadTextFile(
            question, fineTuningExamples.toJsonL(), "fine-tune", ".jsonl");
    return openAiApiHandler.triggerFineTuning(fileId).getFineTunedModel();
  }

  public Optional<NoteDetailsCompletion> getTextFromAudio(
      String modelName,
      String transcriptionFromAudio,
      String additionalInstructions,
      String previousContent) {

    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder = getOpenAIChatRequestBuilder(modelName);

    if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
      chatAboutNoteRequestBuilder.addSystemMessage(
          "Additional instruction:\n" + additionalInstructions);
    }

    if (previousContent != null && !previousContent.isEmpty()) {
      try {
        String jsonContent =
            String.format(
                "{\"previousNoteDetailsToAppendTo\": %s}",
                new com.odde.doughnut.configs.ObjectMapperConfig()
                    .objectMapper()
                    .writeValueAsString(previousContent));
        chatAboutNoteRequestBuilder.addUserMessage(
            "Previous note details (in JSON format):\n" + jsonContent);
      } catch (JsonProcessingException e) {
        return Optional.empty();
      }
    }

    InstructionAndSchema questionEvaluationAiTool =
        AiToolFactory.transcriptionToTextAiTool(transcriptionFromAudio);
    return openAiApiHandler
        .requestAndGetJsonSchemaResult(questionEvaluationAiTool, chatAboutNoteRequestBuilder)
        .flatMap(
            jsonNode -> {
              try {
                ObjectMapper mapper =
                    new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return Optional.of(mapper.treeToValue(jsonNode, NoteDetailsCompletion.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static OpenAIChatRequestBuilder getOpenAIChatRequestBuilder(String modelName) {
    return new OpenAIChatRequestBuilder().model(modelName);
  }

  public String getTranscriptionFromAudio(String filename, byte[] bytes) throws IOException {
    RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), bytes);

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

  public OpenAiOngoingRun resumeRun(String threadId, String runId) {
    return new OpenAiRunResumed(openAiApiHandler, threadId, runId, null);
  }
}
