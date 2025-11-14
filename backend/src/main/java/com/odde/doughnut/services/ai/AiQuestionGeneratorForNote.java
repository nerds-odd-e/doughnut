package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.Optional;

public record AiQuestionGeneratorForNote(
    OpenAiApiHandler openAiApiHandler, OpenAIChatRequestBuilder chatAboutNoteRequestBuilder) {

  public Optional<MCQWithAnswer> refineQuestion(MCQWithAnswer question) {
    InstructionAndSchema questionEvaluationAiTool = AiToolFactory.questionRefineAiTool(question);
    return openAiApiHandler
        .requestAndGetJsonSchemaResult(questionEvaluationAiTool, chatAboutNoteRequestBuilder)
        .flatMap(
            jsonNode -> {
              try {
                return Optional.of(
                    new ObjectMapperConfig()
                        .objectMapper()
                        .treeToValue(jsonNode, MCQWithAnswer.class));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
