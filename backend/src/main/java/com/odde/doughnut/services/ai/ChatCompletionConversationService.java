package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import io.reactivex.Flowable;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatCompletionConversationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final ObjectMapper objectMapper;

  public ChatCompletionConversationService(
      OpenAiApiHandler openAiApiHandler,
      GlobalSettingsService globalSettingsService,
      ObjectMapper objectMapper) {
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.objectMapper = objectMapper;
  }

  public SseEmitter getReplyStream(
      Conversation conversation, ConversationService conversationService) {
    // Build conversation history from database
    ConversationHistoryBuilder historyBuilder = new ConversationHistoryBuilder(objectMapper);
    List<ChatMessage> history = historyBuilder.buildHistory(conversation);

    // Create chat completion request
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model(modelName).messages(history).build();

    // Stream the response
    Flowable<com.theokanning.openai.completion.chat.ChatCompletionChunk> stream =
        openAiApiHandler.streamChatCompletion(request);

    // Convert to SSE and save AI response when complete
    ChatCompletionStream chatStream = new ChatCompletionStream(stream);
    return chatStream.getSseEmitter(
        content -> {
          // Save AI response to database when streaming completes
          conversationService.addMessageToConversation(conversation, null, content);
        });
  }
}
