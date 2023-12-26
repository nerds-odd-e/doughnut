package com.odde.doughnut.services.ai;

import com.theokanning.openai.completion.chat.*;

public class OpenAIChatAboutNoteRequestBuilder1 extends OpenAIChatAboutNoteRequestBuilder {
  public OpenAIChatAboutNoteRequestBuilder1(String modelName) {
    openAIChatRequestBuilder.model(modelName);
    this.openAIChatRequestBuilder.addTextMessage(
      ChatMessageRole.SYSTEM,
      "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
  }
}

