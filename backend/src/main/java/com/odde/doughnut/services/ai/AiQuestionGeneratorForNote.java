package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;

public class AiQuestionGeneratorForNote {
  private final OpenAiApiHandler openAiApiHandler;
  private final OpenAIChatRequestBuilder chatAboutNoteRequestBuilder;

  public AiQuestionGeneratorForNote(
      OpenAiApiHandler openAiApiHandler, OpenAIChatRequestBuilder chatAboutNoteRequestBuilder) {
    this.openAiApiHandler = openAiApiHandler;
    this.chatAboutNoteRequestBuilder = chatAboutNoteRequestBuilder;
  }

  public MCQWithAnswer getAiGeneratedQuestion() {
    AiToolList tool = AiToolFactory.mcqWithAnswerAiTool();
    return requestAndGetFunctionCallArguments(tool)
        .flatMap(MCQWithAnswer::getValidQuestion)
        .orElse(null);
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
