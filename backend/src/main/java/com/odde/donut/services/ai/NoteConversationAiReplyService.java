package com.odde.donut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import com.odde.donut.services.ConversationService;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.ResponseCreateParams;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
@Service
public class NoteConversationAiReplyService {
  private final OpenAiApiHandler openAiApiHandler;
  private final NotebookGitBindingRepository bindingRepository;
  private final ObjectMapper objectMapper;
  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  public ResponseCreateParams buildResponseCreateParams(Conversation conversation) {
    ConversationAiRequestBuilder requestBuilder =
        new ConversationAiRequestBuilder(
            focusContextRetrievalService, focusContextMarkdownRenderer);
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    return requestBuilder.buildResponseCreateParams(conversation, modelName);
  }

  public SseEmitter getReplyStream(
      Conversation conversation, ConversationService conversationService)
      throws OpenAiUnauthorizedException {
    Note subject = conversation.getSubjectNote();
    String baseRevision =
        bindingRepository
            .findByNotebook_Id(subject.getNotebook().getId())
            .map(NotebookGitBinding::getAcceptedGitObjectId)
            .orElse(null);
    String responseId = UUID.randomUUID().toString();
    ResponseCreateParams request = buildResponseCreateParams(conversation);
    Flowable<String> stream = openAiApiHandler.streamResponseAsLegacyChatChunks(request);
    ConversationAiReplySseStream replySseStream = new ConversationAiReplySseStream(stream);
    return replySseStream.getSseEmitter(
        reply -> {
          List<ConversationNoteCorrection> corrections = new ArrayList<>();
          for (ConversationAiReplySseStream.ToolCall tool : reply.toolCalls()) {
            ConversationNoteCorrection.Field field;
            String valueProperty;
            if ("NoteContentCompletion".equals(tool.name())) {
              field = ConversationNoteCorrection.Field.CONTENT;
              valueProperty = "content";
            } else if ("TitleReplacement".equals(tool.name())) {
              field = ConversationNoteCorrection.Field.TITLE;
              valueProperty = "newTitle";
            } else {
              continue;
            }
            try {
              JsonNode arguments = objectMapper.readTree(tool.arguments());
              ConversationNoteCorrection correction = new ConversationNoteCorrection();
              correction.setNote(subject);
              correction.setResponseId(responseId);
              correction.setToolCallId(tool.id());
              correction.setField(field);
              correction.setProposedValue(arguments.path(valueProperty).asText());
              correction.setReason(arguments.path("reason").asText(reply.content()));
              correction.setBaseRevision(baseRevision);
              corrections.add(correction);
            } catch (JsonProcessingException e) {
              throw new IllegalArgumentException("Invalid note correction arguments", e);
            }
          }
          conversationService.retainAiReply(conversation, reply.content(), corrections);
        });
  }
}
