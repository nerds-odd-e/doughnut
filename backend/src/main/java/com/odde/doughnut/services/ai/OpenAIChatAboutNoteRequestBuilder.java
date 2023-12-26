package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Note;
import com.theokanning.openai.completion.chat.*;

public class OpenAIChatAboutNoteRequestBuilder extends OpenAIChatAboutNoteRequestBuilderBase {
  public OpenAIChatAboutNoteRequestBuilder(String modelName, Note note) {
    openAIChatRequestBuilder.model(modelName);
    this.openAIChatRequestBuilder.addTextMessage(
        ChatMessageRole.SYSTEM,
        "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
    String noteOfCurrentFocus = note.getNoteDescription();
    rawNoteContent(noteOfCurrentFocus);
  }
}
