package com.odde.doughnut.services.ai;

public class OpenAIChatAboutNoteFineTuningBuilder extends OpenAIChatAboutNoteRequestBuilderBase {
  public OpenAIChatAboutNoteFineTuningBuilder(String preservedNoteContent) {
    openAIChatRequestBuilder.addSystemMessage(preservedNoteContent);
  }
}
