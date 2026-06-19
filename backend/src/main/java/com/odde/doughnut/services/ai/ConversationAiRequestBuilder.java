package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.openai.models.ChatModel;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import java.util.ArrayList;
import java.util.List;

public class ConversationAiRequestBuilder {
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  public ConversationAiRequestBuilder(
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  public ResponseCreateParams buildResponseCreateParams(
      Conversation conversation, String modelName) {
    List<ResponseInputItem> inputItems = new ArrayList<>();
    addNoteContext(inputItems, conversation);
    for (ConversationMessage msg : conversation.getConversationMessages()) {
      if (msg.getSender() == null) {
        inputItems.add(
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(EasyInputMessage.Role.ASSISTANT)
                    .content(msg.getMessage())
                    .build()));
      } else {
        inputItems.add(
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(EasyInputMessage.Role.USER)
                    .content(msg.getMessage())
                    .build()));
      }
    }

    ResponseCreateParams.Builder builder =
        ResponseCreateParams.builder().model(ChatModel.of(modelName)).inputOfResponse(inputItems);
    for (Class<?> toolClass : AiToolFactory.getAllAssistantTools()) {
      @SuppressWarnings("unchecked")
      Class<Object> paramClass = (Class<Object>) toolClass;
      builder.addTool(paramClass);
    }
    return builder.build();
  }

  private void addNoteContext(List<ResponseInputItem> items, Conversation conversation) {
    Note note = conversation.getSubjectNote();
    if (note == null) {
      return;
    }
    RetrievalConfig config = RetrievalConfig.defaultMaxDepth();
    User viewer = conversation.getConversationInitiator();
    FocusContextResult focusContextResult =
        viewer != null
            ? focusContextRetrievalService.retrieve(note, viewer, config)
            : focusContextRetrievalService.retrieve(note, config);
    String noteDescription = focusContextMarkdownRenderer.render(focusContextResult, config);
    String notebookInstructions = note.getNotebookAssistantInstructions();

    String systemMessageContent = noteDescription;
    if (notebookInstructions != null && !notebookInstructions.trim().isEmpty()) {
      systemMessageContent += "\n\n" + notebookInstructions;
    }

    items.add(
        ResponseInputItem.ofEasyInputMessage(
            EasyInputMessage.builder()
                .role(EasyInputMessage.Role.DEVELOPER)
                .content(systemMessageContent)
                .build()));

    String additionalContext = conversation.getAdditionalContextForSubject();
    if (additionalContext != null) {
      items.add(
          ResponseInputItem.ofEasyInputMessage(
              EasyInputMessage.builder()
                  .role(EasyInputMessage.Role.DEVELOPER)
                  .content(additionalContext)
                  .build()));
    }

    items.add(
        ResponseInputItem.ofEasyInputMessage(
            EasyInputMessage.builder()
                .role(EasyInputMessage.Role.DEVELOPER)
                .content("Make tool calls when user asks to update the note.")
                .build()));
  }
}
