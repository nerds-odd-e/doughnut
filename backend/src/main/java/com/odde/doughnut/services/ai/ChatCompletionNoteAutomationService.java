package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.Optional;

public class ChatCompletionNoteAutomationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final Note note;

  public ChatCompletionNoteAutomationService(
      OpenAiApiHandler openAiApiHandler, GlobalSettingsService globalSettingsService, Note note) {
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.note = note;
  }

  public String suggestTitle() throws JsonProcessingException {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    OpenAIChatRequestBuilder chatRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    chatRequestBuilder.responseJsonSchema(AiToolFactory.suggestNoteTitleAiTool());

    Optional<JsonNode> result =
        openAiApiHandler.requestAndGetJsonSchemaResult(
            AiToolFactory.suggestNoteTitleAiTool(), chatRequestBuilder);

    if (result.isEmpty()) {
      return null;
    }

    try {
      TitleReplacement titleReplacement =
          new ObjectMapperConfig().objectMapper().treeToValue(result.get(), TitleReplacement.class);
      if (titleReplacement != null && titleReplacement.getNewTitle() != null) {
        return titleReplacement.getNewTitle();
      }
      return null;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
