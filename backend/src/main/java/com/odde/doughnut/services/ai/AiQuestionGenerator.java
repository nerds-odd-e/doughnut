package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import java.util.Optional;

public class AiQuestionGenerator {
  private final Note note;

  private final OpenAiApiHandler openAiApiHandler;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler) {
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
  }

  public MCQWithAnswer getAiGeneratedQuestion(String modelName)
      throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder1(modelName, note)
            .userInstructionToGenerateQuestionWithFunctionCall()
            .maxTokens(1500)
            .build();
    JsonNode question = openAiApiHandler
      .getFunctionCallArguments(chatRequest)
      .orElseThrow(QuizQuestionNotPossibleException::new);
    return MCQWithAnswer.getValidQuestion(question);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question, String modelName) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder1(modelName, note)
            .evaluateQuestion(question)
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }

}
