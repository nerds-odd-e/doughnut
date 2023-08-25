package com.odde.doughnut.services;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final OpenAiApiHandler openAiApiHandler;

  public ChatService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String chatToAi(String question) {
    List<ChatMessage> messages =
        List.of(
            new ChatMessage(ChatMessageRole.USER.value(), ""),
            new ChatMessage(ChatMessageRole.ASSISTANT.value(), question));

    ChatCompletionRequest request = generateChatCompletionRequest(messages);

    Optional<ChatCompletionChoice> response = openAiApiHandler.chatCompletion(request);
    if (response.isPresent()) {
      return response.get().getMessage().getContent();
    }
    return "";
  }

  private static ChatCompletionRequest generateChatCompletionRequest(List<ChatMessage> messages) {
    return ChatCompletionRequest.builder().model("gpt-4").messages(messages).stream(false)
        .n(1)
        .maxTokens(100)
        .build();
  }
}
