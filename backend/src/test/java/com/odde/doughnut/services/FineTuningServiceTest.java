package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FineTuningServiceTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private FineTuningService fineTuningService;
  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void setup() {
    fineTuningService = new FineTuningService(this.modelFactoryService, openAiApi);
  }

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
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).positive().please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertEquals(1, goodOpenAIChatGPTFineTuningExampleList.size());
      List<ChatMessageForFineTuning> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(goodTrainingData.get(0).getContent(), containsString("Test Topic"));
      assertThat(
          goodTrainingData.get(1).getContent(),
          containsString("assume the role of a Memory Assistant"));
    }

    @Test
    void shouldIncludeTheQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .positive()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the raw Json question").please())
          .please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      List<ChatMessageForFineTuning> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getFunctionCall().getName(),
          containsString("ask_single_answer_multiple_choice_question"));
      assertThat(
          goodTrainingData.get(2).getFunctionCall().getArguments().toString(),
          containsString("This is the raw Json question"));
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
