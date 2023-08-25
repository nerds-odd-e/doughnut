package com.odde.doughnut.services;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private final OpenAiApiHandler openAiApiHandler;

  public ChatService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String askChatGPT(String askStatement) {
    List<ChatMessage> messages = new ArrayList<>();
    ChatMessage message1 = new ChatMessage(ChatMessageRole.USER.value(), "");
    ChatMessage message2 = new ChatMessage(ChatMessageRole.ASSISTANT.value(), askStatement);
    messages.add(message1);
    messages.add(message2);

    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model("gpt-4").messages(messages).stream(false)
            .n(1)
            .maxTokens(100)
            .build();

    return openAiApiHandler.getOpenAiAnswer(request);
  }
}
