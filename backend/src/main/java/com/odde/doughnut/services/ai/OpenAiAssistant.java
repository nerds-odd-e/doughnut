package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import java.util.ArrayList;
import java.util.List;

public final class OpenAiAssistant {
  private final OpenAiApiHandler openAiApiHandler;
  private final String assistantId;

  public OpenAiAssistant(OpenAiApiHandler openAiApiHandler, String assistantId) {
    this.openAiApiHandler = openAiApiHandler;
    this.assistantId = assistantId;
  }

  public AssistantThread getThread(String threadId) {
    return new AssistantThread(assistantId, threadId, openAiApiHandler);
  }

  public AssistantThread createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages =
        new ArrayList<>(
            List.of(
                MessageRequest.builder()
                    .role("assistant")
                    .content("Please only call function to update content when user asks to.")
                    .build()));

    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    ThreadRequest threadRequest = ThreadRequest.builder().messages(messages).build();
    String threadId = openAiApiHandler.createThread(threadRequest).getId();
    return getThread(threadId);
  }
}
