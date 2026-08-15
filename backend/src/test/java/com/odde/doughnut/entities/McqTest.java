package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.repositories.McqRepository;
import com.odde.doughnut.services.McqService;
import com.odde.doughnut.services.ai.GeneratedMcq;
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
class McqTest {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired McqService mcqService;
  @Autowired McqRepository mcqRepository;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  private GeneratedMcq anUnshuffledGeneratedMcq() {
    return makeMe.aGeneratedMcq().choicesMayBeShuffled(false).please();
  }

  private static QuestionEvaluation evaluation(
      boolean feasible, int[] correctChoices, String advice) {
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = feasible;
    evaluation.correctChoices = correctChoices;
    evaluation.improvementAdvices = advice;
    return evaluation;
  }

  private static QuestionEvaluation accepting(GeneratedMcq mcq) {
    return evaluation(true, new int[] {mcq.getCorrectAnswerIndex()}, "");
  }

  private void stubAcceptedGeneration(GeneratedMcq mcq) {
    openAiStructuredResponseMock.stubStructuredResponse(mcq);
    openAiStructuredResponseMock.stubStructuredResponse(accepting(mcq));
  }

  @Nested
  class AutoEvaluateAndRegenerate {
    Note note;
    GeneratedMcq generatedMcq;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      generatedMcq = anUnshuffledGeneratedMcq();
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
    void shouldRegenerateQuestionWhenEvaluationShowsNotFeasible() {
      GeneratedMcq regeneratedQuestion =
          makeMe.aGeneratedMcq().stem("regenerated stem").choicesMayBeShuffled(false).please();
      openAiStructuredResponseMock.enqueueStructuredResponse(generatedMcq);
      openAiStructuredResponseMock.enqueueStructuredResponse(regeneratedQuestion);
      openAiStructuredResponseMock.enqueueStructuredResponse(
          evaluation(false, new int[] {}, "not feasible"));
      openAiStructuredResponseMock.enqueueStructuredResponse(accepting(regeneratedQuestion));

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
    }
  }
}
