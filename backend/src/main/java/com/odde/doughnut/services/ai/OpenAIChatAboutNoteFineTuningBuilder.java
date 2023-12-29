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

  public OpenAIChatAboutNoteFineTuningBuilder addToolAndToolCall(AiTool tool, Object arguments) {
    openAIChatRequestBuilder.functions.add(tool.getFunction());
    openAIChatRequestBuilder.addUserMessage(tool.getUserRequestMessage());
    openAIChatRequestBuilder.addFunctionCallMessage(arguments, tool.getFunctionName());
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return openAIChatRequestBuilder.buildMessages();
  }
}
