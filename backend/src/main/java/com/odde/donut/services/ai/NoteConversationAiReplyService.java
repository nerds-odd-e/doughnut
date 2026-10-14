package com.odde.donut.services.ai;

import com.odde.donut.entities.Conversation;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import com.odde.donut.services.ConversationService;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
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

    ConversationAiReplySseStream replySseStream = new ConversationAiReplySseStream(stream);
    return replySseStream.getSseEmitter(
        content -> {
          if (content != null && !content.isEmpty()) {
            conversationService.addMessageToConversation(conversation, null, content);
          }
        });
  }
}
