package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

public class AiQuestionGenerator {
  private final Note note;
  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public AIGeneratedQuestion getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    try {
      final AIGeneratedQuestion question = getValidQuestion(false);
      if (question.confidence > 9) return question;
      if (questionMakeSense(question)) {
        question.stem += " (confidence: " + question.confidence + ". Reevaluated.)";
        return question;
      }
    } catch (QuizQuestionNotPossibleException e) {
    }
    final AIGeneratedQuestion gpt4question = getValidQuestion(true);
    gpt4question.stem += " (GPT-4)";
    return gpt4question;
  }

  private Boolean questionMakeSense(AIGeneratedQuestion question) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .validateQuestionAgain(question)
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(
            jsonNode ->
                OpenAIChatAboutNoteRequestBuilder.QuestionEvaluation.getQuestionEvaluation(
                    jsonNode))
        .map(eq -> eq.makeSense(question.correctChoiceIndex))
        .orElse(false);
  }

  private AIGeneratedQuestion getValidQuestion(boolean useGPT4)
      throws QuizQuestionNotPossibleException {
    return AIGeneratedQuestion.getValidQuestion(getAiGeneratedQuestionJson(useGPT4));
  }

  private JsonNode getAiGeneratedQuestionJson(boolean shortContent)
      throws QuizQuestionNotPossibleException {
    OpenAIChatAboutNoteRequestBuilder openAIChatAboutNoteRequestBuilder =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
            .maxTokens(1500);
    if (shortContent) {
      openAIChatAboutNoteRequestBuilder.useGPT4();
    }
    ChatCompletionRequest chatRequest = openAIChatAboutNoteRequestBuilder.build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }
}
