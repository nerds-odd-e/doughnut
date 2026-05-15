package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class OtherAiServices {
  private final OpenAiApiHandler openAiApiHandler;

  public OtherAiServices(OpenAiApiHandler openAiApiHandler) {
    this.openAiApiHandler = openAiApiHandler;
  }

  public List<String> getAvailableGptModels() {
    List<String> modelVersionOptions = new ArrayList<>();

    openAiApiHandler
        .getModels()
        .forEach(
            (e) -> {
              if (e.id().startsWith("ft:") || e.id().startsWith("gpt")) {
                modelVersionOptions.add(e.id());
              }
            });

    return modelVersionOptions;
  }

  public Optional<NoteContentCompletion> getTextFromAudio(
      String modelName,
      String transcriptionFromAudio,
      String additionalInstructions,
      String previousContent) {

    InstructionAndSchema tool = AiToolFactory.transcriptionToTextAiTool(transcriptionFromAudio);
    OpenAIResponseRequestBuilder<NoteContentCompletion> builder =
        new OpenAIResponseRequestBuilder<>(NoteContentCompletion.class).model(modelName);

    if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
      builder.addInstruction("Additional instruction:\n" + additionalInstructions);
    }
    builder.addInstruction(tool.getMessageBody());

    if (previousContent != null && !previousContent.isEmpty()) {
      try {
        String jsonContent =
            String.format(
                "{\"previousNoteContentToAppendTo\": %s}",
                new ObjectMapperConfig().objectMapper().writeValueAsString(previousContent));
        builder.addUserMessage("Previous note content (in JSON format):\n" + jsonContent);
      } catch (JsonProcessingException e) {
        return Optional.empty();
      }
    }

    StructuredResponseCreateParams<NoteContentCompletion> params = builder.build();
    return openAiApiHandler.requestAndGetStructuredResponseResult(params);
  }

  public String getTranscriptionFromAudio(String filename, byte[] bytes) throws IOException {
    return openAiApiHandler.getTranscription(filename, bytes);
  }
}
