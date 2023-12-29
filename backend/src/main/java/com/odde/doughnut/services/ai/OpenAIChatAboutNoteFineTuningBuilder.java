package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class OpenAIChatAboutNoteFineTuningBuilder {
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder =
      new OpenAIChatRequestBuilder();

  public OpenAIChatAboutNoteFineTuningBuilder(String preservedNoteContent) {
    openAIChatRequestBuilder.addSystemMessage(preservedNoteContent);
  }

  public OpenAIChatAboutNoteFineTuningBuilder addToolAndToolCall(
      AiToolList tool, Object arguments) {
    openAIChatRequestBuilder.functions.addAll(tool.getFunctions());
    openAIChatRequestBuilder.addUserMessage(tool.getUserRequestMessage());
    openAIChatRequestBuilder.addFunctionCallMessage(arguments, tool.getFirstFunctionName());
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return openAIChatRequestBuilder.buildMessages();
  }
}
