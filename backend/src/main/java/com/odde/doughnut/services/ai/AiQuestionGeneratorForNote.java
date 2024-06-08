package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;

public record AiQuestionGeneratorForNote(
    OpenAiApiHandler openAiApiHandler, OpenAIChatRequestBuilder chatAboutNoteRequestBuilder) {

  public MCQWithAnswer getAiGeneratedQuestion() {
    AiToolList tool = AiToolFactory.mcqWithAnswerAiTool();
    return requestAndGetFunctionCallArguments(tool)
        .flatMap(AiQuestionGeneratorForNote::getValidQuestion)
        .orElse(null);
  }

  private static Optional<MCQWithAnswer> getValidQuestion(JsonNode question) {
    try {
      MCQWithAnswer mcqWithAnswer = new ObjectMapper().treeToValue(question, MCQWithAnswer.class);
      if (mcqWithAnswer.multipleChoicesQuestion.stem != null
          && !Strings.isBlank(mcqWithAnswer.multipleChoicesQuestion.stem)) {
        return Optional.of(mcqWithAnswer);
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question) {
    AiToolList questionEvaluationAiTool = AiToolFactory.questionEvaluationAiTool(question);
    return requestAndGetFunctionCallArguments(questionEvaluationAiTool)
        .flatMap(QuestionEvaluation::getQuestionEvaluation);
  }

  private Optional<JsonNode> requestAndGetFunctionCallArguments(AiToolList tool) {
    ChatCompletionRequest chatRequest =
        chatAboutNoteRequestBuilder.addTool(tool).maxTokens(1500).build();
    return openAiApiHandler.getFirstToolCallArguments(chatRequest);
  }
}
