package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
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
  private OpenAIAssistantMocker openAIAssistantMocker;
  private OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, globalSettingsService, new NonRandomizer());
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation(null);
  }

  @Nested
  class ContestQuestion {
    PredefinedQuestion predefinedQuestion;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.explanation = "what a horrible question!";

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
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(questionEvaluation, "evaluate_question")
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contest =
          aiQuestionGenerator.getQuestionContestResult(predefinedQuestion);
      assertTrue(contest.rejected);
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(questionEvaluation, "evaluate_question")
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contest =
          aiQuestionGenerator.getQuestionContestResult(predefinedQuestion);
      assertFalse(contest.rejected);
    }

    @Test
    void noFunctionCallInvoked() throws JsonProcessingException {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunWithNoToolCalls()
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      assertThrows(
          RuntimeException.class,
          () -> aiQuestionGenerator.getQuestionContestResult(predefinedQuestion));
    }
  }
}
