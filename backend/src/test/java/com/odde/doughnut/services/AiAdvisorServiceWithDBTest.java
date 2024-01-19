package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.client.OpenAiApi;
import org.assertj.core.api.Assertions;
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
class AiAdvisorServiceWithDBTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class ContestQuestion {
    private OpenAIChatCompletionMock openAIChatCompletionMock;
    QuizQuestionEntity quizQuestionEntity;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.comment = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctChoiceIndex(0)
              .please();
      Note note = makeMe.aNote().please();
      quizQuestionEntity =
          makeMe.aQuestion().ofAIGeneratedQuestion(aiGeneratedQuestion, note.getThing()).please();
    }

    @Test
    void rejected() {
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(questionEvaluation, "");
      QuizQuestionContestResult contest =
          aiAdvisorService.contestQuestion(quizQuestionEntity, "gpt-4");
      assertTrue(contest.rejected);
      Assertions.assertThat(contest.reason)
          .isEqualTo("This seems to be a legitimate question. Please answer it.");
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(questionEvaluation, "");
      QuizQuestionContestResult contest =
          aiAdvisorService.contestQuestion(quizQuestionEntity, "gpt-4");
      assertFalse(contest.rejected);
    }

    @Test
    void noFunctionCallInvoked() throws JsonProcessingException {
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCallJsonNode(
          new ObjectMapper().readTree(""), "");
      assertThrows(
          RuntimeException.class,
          () -> aiAdvisorService.contestQuestion(quizQuestionEntity, "gpt-4"));
    }
  }
}
