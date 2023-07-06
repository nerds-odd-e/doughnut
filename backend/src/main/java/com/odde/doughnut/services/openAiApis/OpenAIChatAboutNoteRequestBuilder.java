package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class OpenAIChatAboutNoteRequestBuilder {
  public List<ChatMessage> messages = new ArrayList<>();
  private String path;
  private int maxTokens;

  private ChatFunction askSingleAnswerMultipleChoiceQuestion = null;

  public OpenAIChatAboutNoteRequestBuilder(String notePath) {
    this.path = notePath;
    String content =
        ("This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
                + "Current context of the note: ")
            + this.path;
    messages.add(0, new ChatMessage(ChatMessageRole.SYSTEM.value(), content));
  }

  public OpenAIChatAboutNoteRequestBuilder detailsOfNoteOfCurrentFocus(Note note) {
    String noteOfCurrentFocus =
        """
The note of current focus:
context: %s
title: %s
description (until the end of this message):
%s
      """
            .formatted(this.path, note.getTitle(), note.getTextContent().getDescription());
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), noteOfCurrentFocus));
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder userInstructionToGenerateQuestion(
      Note note, QuizQuestion question) {

    askSingleAnswerMultipleChoiceQuestion =
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(AIGeneratedQuestion.class, null)
            .build();

    String point6 =
        Strings.isBlank(note.getNoteAccessories().getQuestionGenerationInstruction())
            ? ""
            : "6. Generate the question that is related to "
                + note.getNoteAccessories().getQuestionGenerationInstruction();

    messages.add(
        new ChatMessage(
            ChatMessageRole.USER.value(),
            """
Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

1. Generate a multiple-choice question based on the note in the current context
2. Only the top-level context is visible to the user, use background if more information is needed to make the question stem clear.
3. Provide 2 to 4 choices with only 1 correct answer.
4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.
%s

Note: Only the top-level context is visible. The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
"""
                .formatted(point6)));

    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForCompletion(
      AiCompletionRequest aiCompletionRequest) {
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), aiCompletionRequest.prompt));
    if (!Strings.isEmpty(aiCompletionRequest.incompleteContent)) {
      messages.add(
          new ChatMessage(
              ChatMessageRole.ASSISTANT.value(), aiCompletionRequest.incompleteContent));
    }
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    this.maxTokens = maxTokens;
    return this;
  }

  public ChatCompletionRequest build() {
    ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-16k")
            .messages(messages)
            //
            // an effort has been made to make the api call more responsive by using stream(true)
            // however, due to the library limitation, we cannot do it yet.
            // find more details here:
            //    https://github.com/TheoKanning/openai-java/issues/83
            .stream(false)
            .n(1);
    if (askSingleAnswerMultipleChoiceQuestion != null) {
      requestBuilder.functions(List.of(askSingleAnswerMultipleChoiceQuestion));
    }
    return requestBuilder.maxTokens(maxTokens).build();
  }
}
