package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;

public class AiQuestionGenerator {
  private final OpenAiApiHandler openAiApiHandler;
  private final OpenAIChatRequestBuilder chatAboutNoteRequestBuilder;

  public AiQuestionGenerator(Note note, OpenAiApiHandler openAiApiHandler, String modelName) {
    this.chatAboutNoteRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);
    this.openAiApiHandler = openAiApiHandler;
  }

  public MCQWithAnswer getAiGeneratedQuestion() throws QuizQuestionNotPossibleException {
    AiToolList tool = AiToolFactory.mcqWithAnswerAiTool();
    return requestAndGetFunctionCallArguments(tool)
        .flatMap(MCQWithAnswer::getValidQuestion)
        .orElseThrow(QuizQuestionNotPossibleException::new);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question) {
    AiToolList questionEvaluationAiTool = AiToolFactory.questionEvaluationAiTool(question);
    return requestAndGetFunctionCallArguments(questionEvaluationAiTool)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }

  private Optional<JsonNode> requestAndGetFunctionCallArguments(AiToolList tool) {
    ChatCompletionRequest chatRequest =
        chatAboutNoteRequestBuilder.addTool(tool).maxTokens(1500).build();
    return openAiApiHandler.getFunctionCallArguments(chatRequest);
  }
}
