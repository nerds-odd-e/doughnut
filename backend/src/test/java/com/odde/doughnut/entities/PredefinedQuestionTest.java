package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.repositories.PredefinedQuestionRepository;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
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
class PredefinedQuestionTest {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired PredefinedQuestionService predefinedQuestionService;
  @Autowired PredefinedQuestionRepository predefinedQuestionRepository;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  private MCQWithAnswer anUnshuffledMcq() {
    return makeMe.aMCQWithAnswer().choicesMayBeShuffled(false).please();
  }

  private static QuestionEvaluation evaluation(
      boolean feasible, int[] correctChoices, String advice) {
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = feasible;
    evaluation.correctChoices = correctChoices;
    evaluation.improvementAdvices = advice;
    return evaluation;
  }

  private static QuestionEvaluation accepting(MCQWithAnswer mcq) {
    return evaluation(true, new int[] {mcq.getSolutionChoiceIndex()}, "");
  }

  private void stubAcceptedGeneration(MCQWithAnswer mcq) {
    openAiStructuredResponseMock.stubStructuredResponse(mcq);
    openAiStructuredResponseMock.stubStructuredResponse(accepting(mcq));
  }

  @Nested
  class SpellingQuiz {
    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      Note note = makeMe.aNote().rememberSpelling().please();
      makeMe.aNote("a necessary sibling as filling option").please();
      MCQWithAnswer mcqWithAnswer = anUnshuffledMcq();
      stubAcceptedGeneration(mcqWithAnswer);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(
          result.getMultipleChoicesQuestion().getQuestionStem(),
          containsString(mcqWithAnswer.getQuestion().getQuestionStem()));
    }
  }

  @Nested
  class AutoEvaluateAndRegenerate {
    Note note;
    MCQWithAnswer mcqWithAnswer;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      mcqWithAnswer = anUnshuffledMcq();
    }

    @Test
    void returnsOriginalQuestionWhenEvaluationAcceptsIt() {
      stubAcceptedGeneration(mcqWithAnswer);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(
          result.getMultipleChoicesQuestion().getQuestionStem(),
          equalTo(mcqWithAnswer.getQuestion().getQuestionStem()));
    }

    @Test
    void storesContextSeedOnPredefinedQuestion() {
      stubAcceptedGeneration(mcqWithAnswer);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getContextSeed(), notNullValue());
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationApiFails() {
      openAiStructuredResponseMock.stubStructuredResponse(mcqWithAnswer);
      openAiStructuredResponseMock.stubStructuredResponse(null);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(
          result.getMultipleChoicesQuestion().getQuestionStem(),
          equalTo(mcqWithAnswer.getQuestion().getQuestionStem()));
    }

    @Test
    void shouldRegenerateQuestionWhenEvaluationShowsNotFeasible() {
      MCQWithAnswer regeneratedQuestion =
          makeMe.aMCQWithAnswer().stem("regenerated stem").choicesMayBeShuffled(false).please();
      openAiStructuredResponseMock.enqueueStructuredResponse(mcqWithAnswer);
      openAiStructuredResponseMock.enqueueStructuredResponse(regeneratedQuestion);
      openAiStructuredResponseMock.enqueueStructuredResponse(
          evaluation(false, new int[] {}, "not feasible"));
      openAiStructuredResponseMock.enqueueStructuredResponse(accepting(regeneratedQuestion));

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(
          result.getMultipleChoicesQuestion().getQuestionStem(), equalTo("regenerated stem"));
      assertThat(result.isContested(), is(false));

      PredefinedQuestion contestedOriginal = null;
      for (PredefinedQuestion question : predefinedQuestionRepository.findAll()) {
        if (question.getNote().getId().equals(note.getId()) && question.isContested()) {
          contestedOriginal = question;
          break;
        }
      }
      assertThat(contestedOriginal, notNullValue());
      assertThat(
          contestedOriginal.getMultipleChoicesQuestion().getQuestionStem(),
          equalTo(mcqWithAnswer.getQuestion().getQuestionStem()));
    }
  }
}
