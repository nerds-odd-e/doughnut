package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.services.McqService;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class McqTest extends SpringTestBase {
  @Autowired McqService mcqService;
  @Autowired McqRepository mcqRepository;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.first, 0));
  }

  private static QuestionEvaluation evaluation(
      boolean feasible, int[] correctChoices, String advice) {
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = feasible;
    evaluation.correctChoices = correctChoices;
    evaluation.improvementAdvices = advice;
    return evaluation;
  }

  private static QuestionEvaluation accepting() {
    return evaluation(true, new int[] {0}, "");
  }

  private void stubAcceptedGeneration(GeneratedMcq mcq) {
    openAiStructuredResponseMock.stubStructuredResponse(mcq);
    openAiStructuredResponseMock.stubStructuredResponse(accepting());
  }

  @Nested
  class AutoEvaluateAndRegenerate {
    Note note;
    GeneratedMcq generatedMcq;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      generatedMcq = makeMe.aGeneratedMcq().please();
    }

    @Test
    void returnsOriginalQuestionWhenEvaluationAcceptsIt() {
      stubAcceptedGeneration(generatedMcq);

      Mcq result = mcqService.generateAFeasibleQuestion(note);

      assertThat(result.getQuestionStem(), equalTo(generatedMcq.getQuestionStem()));
    }

    @Test
    void storesContextSeedOnMcq() {
      stubAcceptedGeneration(generatedMcq);

      Mcq result = mcqService.generateAFeasibleQuestion(note);

      assertThat(result.getContextSeed(), notNullValue());
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationApiFails() {
      openAiStructuredResponseMock.stubStructuredResponse(generatedMcq);
      openAiStructuredResponseMock.stubStructuredResponse(null);

      Mcq result = mcqService.generateAFeasibleQuestion(note);

      assertThat(result.getQuestionStem(), equalTo(generatedMcq.getQuestionStem()));
    }

    @Test
    void shouldRegenerateQuestionWhenEvaluatorDisagreesWithoutRewritingOriginalAnswer() {
      GeneratedMcq regeneratedQuestion = makeMe.aGeneratedMcq().stem("regenerated stem").please();
      openAiStructuredResponseMock.enqueueStructuredResponse(generatedMcq);
      openAiStructuredResponseMock.enqueueStructuredResponse(regeneratedQuestion);
      openAiStructuredResponseMock.enqueueStructuredResponse(
          evaluation(true, new int[] {1}, "answer disagreement"));
      openAiStructuredResponseMock.enqueueStructuredResponse(accepting());

      Mcq result = mcqService.generateAFeasibleQuestion(note);

      assertThat(result.getQuestionStem(), equalTo("regenerated stem"));
      assertThat(result.isContested(), is(false));

      Mcq contestedOriginal = null;
      for (Mcq mcq : mcqRepository.findAll()) {
        if (mcq.getNote().getId().equals(note.getId()) && mcq.isContested()) {
          contestedOriginal = mcq;
          break;
        }
      }
      assertThat(contestedOriginal, notNullValue());
      assertThat(contestedOriginal.getQuestionStem(), equalTo(generatedMcq.getQuestionStem()));
      assertThat(contestedOriginal.getCorrectAnswerIndex(), equalTo(0));
    }
  }
}
