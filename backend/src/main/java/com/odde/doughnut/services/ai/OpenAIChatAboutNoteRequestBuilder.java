package com.odde.doughnut.services.ai;

import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.theokanning.openai.completion.chat.*;
import lombok.AllArgsConstructor;

public class OpenAIChatAboutNoteRequestBuilder {
  public static final String askClarificationQuestion = "ask_clarification_question";
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder =
      new OpenAIChatRequestBuilder();

  public OpenAIChatAboutNoteRequestBuilder(String modelName, Note note) {
    openAIChatRequestBuilder.model(modelName);
    this.openAIChatRequestBuilder.addSystemMessage(
        "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
    openAIChatRequestBuilder.addSystemMessage(note.getNoteDescription());
  }

  public OpenAIChatAboutNoteRequestBuilder addTool(AiTool tool) {
    openAIChatRequestBuilder.functions.add(tool.getFunction());
    openAIChatRequestBuilder.addUserMessage(tool.getUserRequestMessage());
    return this;
  }

  @AllArgsConstructor
  public static class UserResponseToClarifyingQuestion {
    public String answerFromUser;
  }

  public OpenAIChatAboutNoteRequestBuilder instructionForDetailsCompletion(
      AiCompletionParams aiCompletionParams) {
    AiToolList aiToolList = AiToolFactory.getNoteContentCompletionTools();
    openAIChatRequestBuilder.functions.addAll(aiToolList.getFunctions());
    openAIChatRequestBuilder.addUserMessage(aiCompletionParams.getCompletionPrompt());
    aiCompletionParams
        .getClarifyingQuestionAndAnswers()
        .forEach(
            qa -> {
              openAIChatRequestBuilder.messages.add(
                  AiToolList.functionCall(askClarificationQuestion, qa.questionFromAI));
              openAIChatRequestBuilder.messages.add(
                  AiToolList.functionCallResponse(
                      askClarificationQuestion,
                      new UserResponseToClarifyingQuestion(qa.answerFromUser)));
            });

    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    openAIChatRequestBuilder.addUserMessage(userMessage);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    openAIChatRequestBuilder.maxTokens(maxTokens);
    return this;
  }

  public ChatCompletionRequest build() {
    return openAIChatRequestBuilder.build();
  }
}
