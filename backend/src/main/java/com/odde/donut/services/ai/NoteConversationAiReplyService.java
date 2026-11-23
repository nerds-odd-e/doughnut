package com.odde.donut.services.ai;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import com.odde.donut.services.ConversationService;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.ResponseCreateParams;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Service
public class NoteConversationAiReplyService {
  private static final JsonMapper objectMapper = ObjectMapperConfig.handWrittenJsonMapper();

  private final OpenAiApiHandler openAiApiHandler;
  private final NotebookGitBindingRepository bindingRepository;
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
    String baseRevision = bindingRepository.findAcceptedRevision(subject.getNotebook().getId());
    String responseId = UUID.randomUUID().toString();
    ResponseCreateParams request = buildResponseCreateParams(conversation);
    ConversationAiReplySseStream replySseStream =
        new ConversationAiReplySseStream(reply -> {
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
              correction.setProposedValue(arguments.path(valueProperty).asString());
              correction.setReason(arguments.path("reason").asString(reply.content()));
              String sourceQuote = arguments.path("sourceQuote").asString(null);
              String sourceUrl = arguments.path("sourceUrl").asString("");
              if (field == ConversationNoteCorrection.Field.CONTENT
                  && sourceQuote != null
                  && (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://"))) {
                correction.setSourceQuote(sourceQuote);
                correction.setSourceUrl(sourceUrl);
              }
              correction.setBaseRevision(baseRevision);
              corrections.add(correction);
            } catch (JacksonException e) {
              throw new IllegalArgumentException("Invalid note correction arguments", e);
            }
          }
          conversationService.retainAiReply(conversation, reply.content(), corrections);
        });
    openAiApiHandler.streamResponseAsLegacyChatChunks(request, replySseStream);
    return replySseStream.getSseEmitter();
  }
}
