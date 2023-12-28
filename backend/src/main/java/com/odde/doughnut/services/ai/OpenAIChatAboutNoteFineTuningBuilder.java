package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class OpenAIChatAboutNoteFineTuningBuilder {
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder =
      new OpenAIChatRequestBuilder();

  public OpenAIChatAboutNoteFineTuningBuilder(String preservedNoteContent) {
    openAIChatRequestBuilder.addSystemMessage(preservedNoteContent);
  }

  public <T> OpenAIChatAboutNoteFineTuningBuilder addToolAndResult(
      T preservedQuestion, AiTool<T> tool) {
    tool.addToolToChatMessages(openAIChatRequestBuilder);
    tool.addFunctionCallToMessages(openAIChatRequestBuilder, preservedQuestion);
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return openAIChatRequestBuilder.buildMessages();
  }
}
