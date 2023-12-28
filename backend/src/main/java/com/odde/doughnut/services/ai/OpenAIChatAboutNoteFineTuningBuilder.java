package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;

public class OpenAIChatAboutNoteFineTuningBuilder extends OpenAIChatAboutNoteRequestBuilderBase {
  public OpenAIChatAboutNoteFineTuningBuilder(String preservedNoteContent) {
    openAIChatRequestBuilder.addSystemMessage(preservedNoteContent);
  }

  public OpenAIChatAboutNoteRequestBuilderBase addToolAndResult(
      MCQWithAnswer preservedQuestion, AiTool<MCQWithAnswer> tool) {
    tool.addToolToChatMessages(openAIChatRequestBuilder);
    tool.addFunctionCallResultToMessages(openAIChatRequestBuilder, preservedQuestion);
    return this;
  }
}
