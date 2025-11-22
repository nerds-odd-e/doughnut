package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.QuestionSuggestionForFineTuningRepository;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.testability.MakeMe;
import com.openai.client.OpenAIClient;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FineTuningServiceTest {
  @Autowired QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;
  @Autowired MakeMe makeMe;
  @Autowired FineTuningService fineTuningService;

  @MockitoBean(name = "testableOpenAiApi")
  private OpenAiApi openAiApi;

  @MockitoBean(name = "officialOpenAiClient")
  private OpenAIClient officialClient;

  @Nested
  class getAllOpenAIChatGPTFineTuningExample {
    @Test
    void shouldIncludeAllFeedbackData_whenCallGetGoodTrainingData() {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
      assertThat(fineTuningService.getQuestionEvaluationTrainingExamples(), hasSize(2));
    }
  }

  @Nested
  class getAllPositiveFeedbackQuestionGenerationFineTuningExamples {

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() {
      List<OpenAIChatGPTFineTuningExample> goodTrainingData =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData() {
      Note note = makeMe.aNote().titleConstructor("Test Title").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).positive().please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertEquals(1, goodOpenAIChatGPTFineTuningExampleList.size());
      List<ChatMessageForFineTuning> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(goodTrainingData.get(0).getContent(), containsString("Test Title"));
      assertThat(
          goodTrainingData.get(1).getContent(),
          containsString("Please act as a Question Designer"));
    }

    @Test
    void shouldIncludeTheQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .positive()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the raw Json question").please())
          .please();
      OpenAIChatGPTFineTuningExample example =
          fineTuningService.getQuestionGenerationTrainingExamples().get(0);
      ChatMessageForFineTuning assistantMessage = example.getMessages().get(2);
      assertThat(assistantMessage.getRole(), equalTo("assistant"));
      assertThat(assistantMessage.getContent(), containsString("This is the raw Json question"));
    }

    @Test
    void shouldIncludeOnlyPositiveQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .negative()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the negative raw Json question").please())
          .please();

      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();

      assertEquals(0, goodOpenAIChatGPTFineTuningExampleList.size());
    }
  }
}
