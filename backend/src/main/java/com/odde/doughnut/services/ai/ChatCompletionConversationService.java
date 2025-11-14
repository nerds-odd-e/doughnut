package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatTool;
import io.reactivex.Flowable;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for handling AI conversations using OpenAI's Chat Completion API.
 *
 * <p>Provides streaming conversation responses with inline tool call handling.
 */
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
      Conversation conversation, ConversationService conversationService)
      throws OpenAiUnauthorizedException {
    // Build conversation history from database
    ConversationHistoryBuilder historyBuilder = new ConversationHistoryBuilder(objectMapper);
    List<ChatMessage> history = historyBuilder.buildHistory(conversation);

    // Get available tools for conversation
    List<AiTool> availableTools = AiToolFactory.getAllAssistantTools();
    List<ChatTool> chatTools = availableTools.stream().map(AiTool::getChatTool).toList();

    // Create chat completion request with tools
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model(modelName).messages(history).tools(chatTools).build();

    // Stream the response
    Flowable<String> stream = openAiApiHandler.streamChatCompletion(request);

    // Convert to SSE and save AI response when complete
    ChatCompletionStream chatStream = new ChatCompletionStream(stream);
    SseEmitter emitter =
        chatStream.getSseEmitter(
            content -> {
              // Save AI response to database when streaming completes (only for text responses)
              if (content != null && !content.isEmpty()) {
                conversationService.addMessageToConversation(conversation, null, content);
              }
            });

    // Note: Errors from the stream will be handled by the error callback in ChatCompletionStream
    // which calls emitter.completeWithError(). If it's an OpenAiUnauthorizedException,
    // it will be caught by the controller's exception handler.
    return emitter;
  }
}
