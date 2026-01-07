package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;

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
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    var tool = AiToolFactory.suggestNoteTitleAiTool();
    chatRequestBuilder.responseJsonSchema(tool);

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(tool, chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                TitleReplacement titleReplacement =
                    new ObjectMapperConfig()
                        .objectMapper()
                        .treeToValue(jsonNode, TitleReplacement.class);
                return titleReplacement.getNewTitle();
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(null);
  }

  public java.util.List<String> generateUnderstandingChecklist() throws JsonProcessingException {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    OpenAIChatRequestBuilder chatRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    var tool = AiToolFactory.generateUnderstandingChecklistAiTool();
    chatRequestBuilder.responseJsonSchema(tool);

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(tool, chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                UnderstandingChecklist understandingChecklist =
                    new ObjectMapperConfig()
                        .objectMapper()
                        .treeToValue(jsonNode, UnderstandingChecklist.class);
                return understandingChecklist.getPoints();
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(java.util.List.of());
  }

  public String removePointFromNote(String pointToRemove) throws JsonProcessingException {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    OpenAIChatRequestBuilder chatRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    var tool = AiToolFactory.removePointFromNoteAiTool(pointToRemove);
    chatRequestBuilder.responseJsonSchema(tool);

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(tool, chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                NoteDetailsRephrase noteDetailsRephrase =
                    new ObjectMapperConfig()
                        .objectMapper()
                        .treeToValue(jsonNode, NoteDetailsRephrase.class);
                return noteDetailsRephrase.getDetails();
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse("");
  }
}
