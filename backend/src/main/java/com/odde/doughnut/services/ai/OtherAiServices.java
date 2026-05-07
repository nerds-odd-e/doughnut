package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
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

    OpenAIChatRequestBuilder chatAboutNoteRequestBuilder = getOpenAIChatRequestBuilder(modelName);

    if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
      chatAboutNoteRequestBuilder.addToOverallSystemMessage(
          "Additional instruction:\n" + additionalInstructions);
    }

    if (previousContent != null && !previousContent.isEmpty()) {
      try {
        String jsonContent =
            String.format(
                "{\"previousNoteContentToAppendTo\": %s}",
                new ObjectMapperConfig().objectMapper().writeValueAsString(previousContent));
        chatAboutNoteRequestBuilder.addUserMessage(
            "Previous note content (in JSON format):\n" + jsonContent);
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
                ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return Optional.of(mapper.treeToValue(jsonNode, NoteContentCompletion.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static OpenAIChatRequestBuilder getOpenAIChatRequestBuilder(String modelName) {
    return new OpenAIChatRequestBuilder().model(modelName);
  }

  public String getTranscriptionFromAudio(String filename, byte[] bytes) throws IOException {
    return openAiApiHandler.getTranscription(filename, bytes);
  }
}
