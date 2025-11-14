package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatCompletionConversationServiceTest {

  @Autowired MakeMe makeMe;
  @Mock OpenAiApiHandler openAiApiHandler;
  @Mock ConversationService conversationService;

  private ObjectMapper objectMapper;
  private ChatCompletionConversationService service;
  private GlobalSettingsService globalSettingsService;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    objectMapper = new ObjectMapperConfig().objectMapper();

    // Use real GlobalSettingsService with makeMe's factory service
    globalSettingsService = new GlobalSettingsService(makeMe.modelFactoryService);

    service =
        new ChatCompletionConversationService(
            openAiApiHandler, globalSettingsService, objectMapper);
  }

  @Test
  void shouldGenerateStreamingResponseForConversation() {
    // Given a conversation with a note
    Note note = makeMe.aNote().please();
    User user = makeMe.aUser().please();
    Conversation conversation = makeMe.aConversation().forANote(note).from(user).please();
    makeMe.aConversationMessage(conversation).sender(user).message("Tell me more").please();

    // Mock the streaming response
    when(openAiApiHandler.streamChatCompletion(any(ChatCompletionRequest.class)))
        .thenReturn(Flowable.empty());

    // When generating a reply
    SseEmitter result = service.getReplyStream(conversation, conversationService);

    // Then should return an emitter
    assertNotNull(result);
    verify(openAiApiHandler).streamChatCompletion(any(ChatCompletionRequest.class));
  }
}
