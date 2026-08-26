package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.entities.Mcq;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NoteQuestionGenerationServiceTests extends NoteQuestionGenerationServiceTestBase {

  @Nested
  class GenerateQuestion {
    @Test
    void shouldGenerateQuestionWithCorrectStem() throws Exception {
      GeneratedMcq generatedMcq =
          makeMe.aGeneratedMcq().stem("What is the first color in the rainbow?").please();
      openAiStructuredResponseMock.stubStructuredResponse(generatedMcq);

      GeneratedMcq result = service.generateQuestion(testNote, null);

      assertThat(
          result.getQuestionStem(), containsString("What is the first color in the rainbow?"));
    }

    @Test
    void shouldUseQuestionGenerationModelNameFromGlobalSettings() throws JsonProcessingException {
      globalSettingsService
          .globalSettingQuestionGeneration()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-question-generation");
      GeneratedMcq generatedMcq = makeMe.aGeneratedMcq().please();
      openAiStructuredResponseMock.stubStructuredResponse(generatedMcq);

      service.generateQuestion(testNote, null);

      ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> paramsCaptor =
          responseParamsCaptor();
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      assertThat(modelName(paramsCaptor.getValue()), is("gpt-question-generation"));
    }

    @Test
    void shouldUseSameRequestShapeAsExportedQuestionGenerationRequest()
        throws JsonProcessingException {
      GeneratedMcq generatedMcq = makeMe.aGeneratedMcq().please();
      openAiStructuredResponseMock.stubStructuredResponse(generatedMcq);

      StructuredResponseCreateParams<GeneratedMcq> exportedRequest =
          service.buildQuestionGenerationRequest(testNote, "Generate a focused question");

      service.generateQuestion(testNote, "Generate a focused question");

      ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> paramsCaptor =
          responseParamsCaptor();
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<GeneratedMcq> runtimeRequest = paramsCaptor.getValue();
      assertThat(modelName(runtimeRequest), is(modelName(exportedRequest)));
      assertThat(inputText(runtimeRequest), is(inputText(exportedRequest)));
      assertThat(instructionText(runtimeRequest), is(instructionText(exportedRequest)));
    }

    @Test
    void shouldReturnNullWhenStructuredResponseIsAbsent() throws JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(null);

      GeneratedMcq result = service.generateQuestion(testNote, null);

      assertThat(result, is(nullValue()));
    }
  }

  @Nested
  class EvaluateQuestion {
    @Test
    void shouldReturnEmptyWhenEvaluationFails() throws Exception {
      Mcq mcq = makeMe.anMcq().please();
      openAiStructuredResponseMock.stubStructuredResponse(null);

      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcq);

      assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldReturnEvaluationWhenEvaluationSucceeds() throws Exception {
      Mcq mcq = makeMe.anMcq().please();
      QuestionEvaluation evaluation = new QuestionEvaluation();
      evaluation.feasibleQuestion = true;
      evaluation.correctChoices = new int[] {0};
      evaluation.improvementAdvices = "Good question";
      openAiStructuredResponseMock.stubStructuredResponse(evaluation);

      Optional<QuestionEvaluation> result = service.evaluateQuestion(testNote, mcq);

      assertThat(result.isPresent(), is(true));
      assertThat(result.get().feasibleQuestion, is(true));
      assertThat(result.get().correctChoices, equalTo(new int[] {0}));
    }

    @Test
    void shouldNotIncludeNotebookQuestionGenerationInstructionInEvaluation()
        throws JsonProcessingException {
      makeMe
          .theNotebook(testNote.getNotebook())
          .readmeContent("---\nquestion_generation_instruction: NOT_FOR_EVALUATION\n---\n")
          .please();
      Mcq mcq = makeMe.anMcq().please();
      QuestionEvaluation evaluation = new QuestionEvaluation();
      evaluation.feasibleQuestion = true;
      openAiStructuredResponseMock.stubStructuredResponse(evaluation);

      service.evaluateQuestion(testNote, mcq);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<QuestionEvaluation>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      String instructions = paramsCaptor.getValue().rawParams().instructions().orElse("");
      assertThat(instructions, not(containsString("NOT_FOR_EVALUATION")));
    }
  }
}
