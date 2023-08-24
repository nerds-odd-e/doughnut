package com.odde.doughnut.services;

import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
  @Autowired private OpenAiApi openAiApi;

  public String askChatGPT(String askStatement) {
    List messages = new ArrayList<ChatMessage>();
    ChatMessage message1 = new ChatMessage(ChatMessageRole.USER.value(), "");
    ChatMessage message2 = new ChatMessage(ChatMessageRole.ASSISTANT.value(), askStatement);
    messages.add(message1);
    messages.add(message2);

    ChatCompletionRequest request =
        ChatCompletionRequest.builder().model("gpt-4").messages(messages).stream(false)
            .n(1)
            .maxTokens(100)
            .build();

    Optional<ChatCompletionChoice> result =
        openAiApi.createChatCompletion(request).blockingGet().getChoices().stream().findFirst();
    return result.get().getMessage().getContent();
  }
}
