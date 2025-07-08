package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiOpenAiAssistantFactoryWithDBTest {

  private AiQuestionGenerator aiQuestionGenerator;
  @Mock private OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  private OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, globalSettingsService, new NonRandomizer(), getTestObjectMapper());
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
  }

  private com.fasterxml.jackson.databind.ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class ContestQuestion {
    PredefinedQuestion predefinedQuestion;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.improvementAdvices = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctChoiceIndex(0)
              .please();
      Note note = makeMe.aNote().please();
      predefinedQuestion =
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestion(aiGeneratedQuestion, note).please();
    }

    @Test
    void rejected() {
      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      MCQWithAnswer mcqWithAnswer = predefinedQuestion.getMcqWithAnswer();
      QuestionContestResult contest =
          aiQuestionGenerator
              .getQuestionContestResult(predefinedQuestion.getNote(), mcqWithAnswer)
              .getQuestionContestResult(mcqWithAnswer);
      assertTrue(contest.rejected);
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      MCQWithAnswer mcqWithAnswer = predefinedQuestion.getMcqWithAnswer();
      QuestionContestResult contest =
          aiQuestionGenerator
              .getQuestionContestResult(predefinedQuestion.getNote(), mcqWithAnswer)
              .getQuestionContestResult(mcqWithAnswer);
      assertFalse(contest.rejected);
    }

    @Test
    void noFunctionCallInvoked() throws JsonProcessingException {
      openAIChatCompletionMock.mockNullChatCompletion();

      assertThrows(
          RuntimeException.class,
          () -> {
            MCQWithAnswer mcqWithAnswer = predefinedQuestion.getMcqWithAnswer();
            aiQuestionGenerator
                .getQuestionContestResult(predefinedQuestion.getNote(), mcqWithAnswer)
                .getQuestionContestResult(mcqWithAnswer);
          });
    }
  }
}
