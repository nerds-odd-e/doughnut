package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.dto.SrtDto;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    String fileId = openAiApiHandler.uploadFineTuningExamples(examples, question);
    return openAiApiHandler.triggerFineTuning(fileId).getFineTunedModel();
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
}
