package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class OpenAIChatAboutNoteMessageBuilder {
  public List<ChatMessage> messages = new ArrayList<>();
  private String path;

  public OpenAIChatAboutNoteMessageBuilder(String notePath) {
    this.path = notePath;
    String content =
        ("This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
                + "Current context of the note: ")
            + this.path;
    messages.add(0, new ChatMessage(ChatMessageRole.SYSTEM.value(), content));
  }

  public static ChatCompletionRequest.ChatCompletionRequestBuilder
      defaultChatCompletionRequestBuilder(List<ChatMessage> messages) {
    return ChatCompletionRequest.builder()
        .model("gpt-3.5-turbo")
        .messages(messages)
        //
        // an effort has been made to make the api call more responsive by using stream(true)
        // however, due to the library limitation, we cannot do it yet.
        // find more details here:
        //    https://github.com/TheoKanning/openai-java/issues/83
        .stream(false)
        .n(1);
  }

  public List<ChatMessage> build() {
    return messages;
  }

  public OpenAIChatAboutNoteMessageBuilder detailsOfNoteOfCurrentFocus(Note note) {
    String noteOfCurrentFocus =
        """
The note of current focus:
title: %s
description (until the end of this message):
%s
      """
            .formatted(note.getTitle(), note.getTextContent().getDescription());
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), noteOfCurrentFocus));
    return this;
  }

  public OpenAIChatAboutNoteMessageBuilder userInstructionToGenerateQuestion() {
    messages.add(
        new ChatMessage(
            ChatMessageRole.USER.value(),
            """
Please note that I don't know which note is of current focus.
To help me recall and refresh my memory about it,
please generate a multiple-choice question with 2 to 4 options and only 1 correct option.
Vary the option text length, so that the correct answer isn't always the longest one.
The question should be about the note of current focus in its context.
Leave the 'question' field empty if you find there's too little information to generate a question.
"""));
    return this;
  }

  public OpenAIChatAboutNoteMessageBuilder instructionForCompletion(
      AiCompletionRequest aiCompletionRequest) {
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), aiCompletionRequest.prompt));
    if (!Strings.isEmpty(aiCompletionRequest.incompleteContent)) {
      messages.add(
          new ChatMessage(
              ChatMessageRole.ASSISTANT.value(), aiCompletionRequest.incompleteContent));
    }
    return this;
  }

  public ChatCompletionRequest buildChatCompletionRequest() {
    return defaultChatCompletionRequestBuilder(messages).maxTokens(100).build();
  }
}
