package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.focusContext.FocusContextFocusNote;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.ResponseCreateParams;
import io.reactivex.Flowable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class NoteConversationAiReplyServiceTest {

  @Mock OpenAiApiHandler openAiApiHandler;
  @Mock GlobalSettingsService globalSettingsService;
  @Mock FocusContextRetrievalService focusContextRetrievalService;
  @Mock FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  @Mock ConversationService conversationService;
  @Mock GlobalSettingsService.GlobalSettingsKeyValue evaluationModelSetting;
  @Mock Note note;
  @Mock User user;
  @Mock Conversation conversation;

  NoteConversationAiReplyService service;

  @BeforeEach
  void setup() {
    service =
        new NoteConversationAiReplyService(
            openAiApiHandler,
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer);
    when(globalSettingsService.globalSettingEvaluation()).thenReturn(evaluationModelSetting);
    when(evaluationModelSetting.getValue()).thenReturn("gpt-4.1-mini");
    when(focusContextRetrievalService.retrieve(any(Note.class), any(User.class), any()))
        .thenReturn(minimalFocusContextResult());
    when(focusContextMarkdownRenderer.render(any(), any())).thenReturn("# Focus Context");
    when(note.getNotebookAssistantInstructions()).thenReturn(null);
    when(conversation.getSubjectNote()).thenReturn(note);
    when(conversation.getConversationInitiator()).thenReturn(user);
    when(conversation.getAdditionalContextForSubject()).thenReturn(null);
  }

  @Test
  void shouldGenerateStreamingResponseForConversation() {
    ConversationMessage conversationMessage = new ConversationMessage();
    conversationMessage.setSender(user);
    conversationMessage.setMessage("Tell me more");
    when(conversation.getConversationMessages()).thenReturn(List.of(conversationMessage));

    when(openAiApiHandler.streamResponseAsLegacyChatChunks(any(ResponseCreateParams.class)))
        .thenReturn(Flowable.empty());

    SseEmitter result = service.getReplyStream(conversation, conversationService);

    assertNotNull(result);
    verify(openAiApiHandler).streamResponseAsLegacyChatChunks(any(ResponseCreateParams.class));
  }

  private static FocusContextResult minimalFocusContextResult() {
    return new FocusContextResult(
        new FocusContextFocusNote(
            "notebook", "title", "", 0, List.of(), List.of(), List.of(), null, "content", false));
  }
}
