package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.ResponseCreateParams;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
@Service
public class NoteConversationAiReplyService {
  private final OpenAiApiHandler openAiApiHandler;
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
    ResponseCreateParams request = buildResponseCreateParams(conversation);

    Flowable<String> stream = openAiApiHandler.streamResponseAsLegacyChatChunks(request);

    ConversationAiReplySseStream chatStream = new ConversationAiReplySseStream(stream);
    return chatStream.getSseEmitter(
        content -> {
          if (content != null && !content.isEmpty()) {
            conversationService.addMessageToConversation(conversation, null, content);
          }
        });
  }
}
