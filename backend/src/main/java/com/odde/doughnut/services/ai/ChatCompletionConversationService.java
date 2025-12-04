package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.GraphRAGService;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import io.reactivex.Flowable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for handling AI conversations using OpenAI's Chat Completion API.
 *
 * <p>Provides streaming conversation responses with inline tool call handling.
 */
@RequiredArgsConstructor
@Service
public class ChatCompletionConversationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final GraphRAGService graphRAGService;

  public ChatCompletionCreateParams buildChatCompletionRequest(Conversation conversation) {
    // Build conversation history from database
    ConversationHistoryBuilder historyBuilder = new ConversationHistoryBuilder(graphRAGService);
    List<ChatCompletionMessageParam> history = historyBuilder.buildHistory(conversation);

    // Get available tools for conversation
    List<Class<?>> availableTools = AiToolFactory.getAllAssistantTools();

    // Create chat completion request with tools
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    ChatCompletionCreateParams.Builder builder =
        ChatCompletionCreateParams.builder().model(ChatModel.of(modelName)).messages(history);

    // Add tools using the builder's addTool(Class) method which generates schema automatically
    for (Class<?> toolClass : availableTools) {
      @SuppressWarnings("unchecked")
      Class<Object> paramClass = (Class<Object>) toolClass;
      builder.addTool(paramClass);
    }

    return builder.build();
  }

  public SseEmitter getReplyStream(
      Conversation conversation, ConversationService conversationService)
      throws OpenAiUnauthorizedException {
    ChatCompletionCreateParams request = buildChatCompletionRequest(conversation);

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
