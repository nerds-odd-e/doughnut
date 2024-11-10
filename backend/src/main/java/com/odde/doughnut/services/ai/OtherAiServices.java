package com.odde.doughnut.services.ai;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.FineTuningExamples;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public record OtherAiServices(OpenAiApiHandler openAiApiHandler) {
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

  public Optional<TextFromAudio> getTextFromAudio(
      String previousTrailingNoteDetails, String filename, byte[] bytes, String modelName)
      throws IOException {
    String transcriptionFromAudio = getTranscriptionFromAudio(filename, bytes);

    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder =
        getOpenAIChatRequestBuilder(previousTrailingNoteDetails, modelName);
    AiToolList questionEvaluationAiTool =
        AiToolFactory.transcriptionToTextAiTool(transcriptionFromAudio);
    return openAiApiHandler
        .requestAndGetFunctionCallArguments(questionEvaluationAiTool, chatAboutNoteRequestBuilder)
        .flatMap(
            jsonNode -> {
              try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return Optional.of(mapper.treeToValue(jsonNode, TextFromAudio.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static OpenAIChatRequestBuilder getOpenAIChatRequestBuilder(
      String previousTrailingNoteDetails, String modelName) {
    String prettyString =
        defaultObjectMapper()
            .valueToTree(
                Map.of(
                    "previousTrailingNoteDetails",
                    previousTrailingNoteDetails == null ? "" : previousTrailingNoteDetails))
            .toPrettyString();
    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addSystemMessage(
            """
      The trailing note details before appending the text from the audio are (in JSON format):

      %s
      """
                .formatted(prettyString));
  }

  public String getTranscriptionFromAudio(String filename, byte[] bytes) throws IOException {
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
}
