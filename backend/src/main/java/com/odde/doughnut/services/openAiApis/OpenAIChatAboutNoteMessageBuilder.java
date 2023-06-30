package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.models.NoteModel;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatAboutNoteMessageBuilder {
  private List<ChatMessage> messages = new ArrayList<>();
  private NoteModel noteModel;

  public OpenAIChatAboutNoteMessageBuilder(NoteModel noteModel) {
    this.noteModel = noteModel;
  }

  public List<ChatMessage> build() {
    String content =
        ("This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
                + "Current context of the note: ")
            + this.noteModel.getPath();
    messages.add(0, new ChatMessage(ChatMessageRole.SYSTEM.value(), content));
    return messages;
  }
}
