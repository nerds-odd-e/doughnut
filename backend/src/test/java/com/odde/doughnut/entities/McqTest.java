package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.repositories.McqRepository;
import com.odde.doughnut.services.McqService;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.odde.doughnut.testability.TestabilitySettings;
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
class McqTest {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired McqService mcqService;
  @Autowired McqRepository mcqRepository;
  @Autowired TestabilitySettings testabilitySettings;

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
