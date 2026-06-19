package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.Optional;

public record AiQuestionGeneratorForNote(
    OpenAiApiHandler openAiApiHandler,
    OpenAIResponseRequestBuilder<MCQWithAnswerForRefinement> responseRequestBuilder) {

  public Optional<MCQWithAnswer> refineQuestion(MCQWithAnswer question) {
    InstructionAndSchema questionEvaluationAiTool = AiToolFactory.questionRefineAiTool(question);
    responseRequestBuilder.addInstruction(questionEvaluationAiTool.getMessageBody());
    return openAiApiHandler
        .requestAndGetStructuredResponseResult(responseRequestBuilder.build())
        .map(refined -> (MCQWithAnswer) refined);
  }
}
