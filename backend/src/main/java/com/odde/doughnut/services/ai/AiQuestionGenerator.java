package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;

public class AiQuestionGenerator {
  private final OpenAiApiHandler openAiApiHandler;
  private final OpenAIChatAboutNoteRequestBuilder chatAboutNoteRequestBuilder;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler, String modelName) {
    this.chatAboutNoteRequestBuilder = new OpenAIChatAboutNoteRequestBuilder(modelName, note);
    this.openAiApiHandler = openAiApiHandler;
  }

  public MCQWithAnswer getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    ChatCompletionRequest chatRequest =
        chatAboutNoteRequestBuilder
            .userInstructionToGenerateQuestionWithFunctionCall()
            .maxTokens(1500)
            .build();
    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(MCQWithAnswer::getValidQuestion)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question) {
    ChatCompletionRequest chatRequest =
        chatAboutNoteRequestBuilder.evaluateQuestion(question).maxTokens(1500).build();

    return openAiApiHandler
        .getFunctionCallArguments(chatRequest)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }
}
