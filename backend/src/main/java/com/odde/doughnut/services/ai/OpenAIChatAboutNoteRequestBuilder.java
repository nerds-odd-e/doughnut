package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class OpenAIChatAboutNoteRequestBuilder {
  String model = "gpt-3.5-turbo-16k";
  private List<ChatMessage> messages = new ArrayList<>();
  private List<ChatFunction> functions = new ArrayList<>();
  private String path;
  private int maxTokens;

  public OpenAIChatAboutNoteRequestBuilder(String notePath) {
    this.path = notePath;
    String content =
        ("This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
                + "Current context of the note: ")
            + this.path;
    addMessage(content, ChatMessageRole.SYSTEM);
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
            .formatted(this.path, note.getTitle(), note.getDescription());
    return addMessage(noteOfCurrentFocus, ChatMessageRole.SYSTEM);
  }

  public OpenAIChatAboutNoteRequestBuilder userInstructionToGenerateQuestion() {
    functions.add(
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(AIGeneratedQuestion.class, null)
            .build());

    String messageBody =
        """
  Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

  1. Generate a multiple-choice question based on the note in the current context
  2. Only the top-level context is visible to the user.
  3. Provide 2 to 4 choices with only 1 correct answer.
  4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
  5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

  Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
  """;
    return addMessage(messageBody, ChatMessageRole.USER);
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForCompletion(
      AiCompletionRequest aiCompletionRequest) {
    addMessage(aiCompletionRequest.prompt, ChatMessageRole.USER);
    if (!Strings.isEmpty(aiCompletionRequest.incompleteContent)) {
      addMessage(aiCompletionRequest.incompleteContent, ChatMessageRole.ASSISTANT);
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
            .model(model)
            .messages(messages)
            //
            // an effort has been made to make the api call more responsive by using stream(true)
            // however, due to the library limitation, we cannot do it yet.
            // find more details here:
            //    https://github.com/TheoKanning/openai-java/issues/83
            .stream(false)
            .n(1);
    if (!functions.isEmpty()) {
      requestBuilder.functions(functions);
    }
    return requestBuilder.maxTokens(maxTokens).build();
  }

  public OpenAIChatAboutNoteRequestBuilder useGPT4() {
    model = "gpt-4";
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder validateQuestionAgain(AIGeneratedQuestion question) {
    functions.add(
        ChatFunction.builder()
            .name("evaluate_question")
            .description("answer and evaluate the feasibility of the question")
            .executor(QuestionEvaluation.class, null)
            .build());

    AIGeneratedQuestionBody clone = new AIGeneratedQuestionBody();
    clone.stem = question.stem;
    clone.choices = question.choices;

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level context is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(clone.toJsonString());
    return addMessage(messageBody, ChatMessageRole.USER);
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    ChatMessageRole role = ChatMessageRole.USER;
    return addMessage(userMessage, role);
  }

  private OpenAIChatAboutNoteRequestBuilder addMessage(String userMessage, ChatMessageRole role) {
    messages.add(new ChatMessage(role.value(), userMessage));
    return this;
  }
}
