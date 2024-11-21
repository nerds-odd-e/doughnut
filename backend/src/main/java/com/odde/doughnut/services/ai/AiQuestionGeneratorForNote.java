package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.Optional;

public record AiQuestionGeneratorForNote(
    OpenAiApiHandler openAiApiHandler, OpenAIChatRequestBuilder chatAboutNoteRequestBuilder) {

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question) {
    AiToolList questionEvaluationAiTool = AiToolFactory.questionEvaluationAiTool(question);
    return openAiApiHandler
        .requestAndGetFunctionCallArguments(questionEvaluationAiTool, chatAboutNoteRequestBuilder)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }

  public Optional<MCQWithAnswer> refineQuestion(MCQWithAnswer question) {
    AiToolList questionEvaluationAiTool = AiToolFactory.questionRefineAiTool(question);
    return openAiApiHandler
        .requestAndGetFunctionCallArguments(questionEvaluationAiTool, chatAboutNoteRequestBuilder)
        .flatMap(
            jsonNode -> {
              try {
                return Optional.of(new ObjectMapper().treeToValue(jsonNode, MCQWithAnswer.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
