package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.services.ai.GeneratedMcq;
import com.openai.models.responses.StructuredResponseCreateParams;
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
    void shouldReturnNullWhenStructuredResponseIsAbsent() throws JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(null);

      GeneratedMcq result = service.generateQuestion(testNote, null);

      assertThat(result, is(nullValue()));
    }
  }
}
