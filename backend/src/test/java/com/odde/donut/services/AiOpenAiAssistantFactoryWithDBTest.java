package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.AiQuestionGenerator;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
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
class AiOpenAiAssistantFactoryWithDBTest {

  @Autowired AiQuestionGenerator aiQuestionGenerator;

  @MockitoBean(name = "officialOpenAiClient")
  private OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  private OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  @Nested
  class ContestQuestion {
    Mcq mcq;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.improvementAdvices = "what a horrible question!";

      Note note = makeMe.aNote().please();
      mcq =
          makeMe
              .anMcq()
              .forNote(note)
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctAnswerIndex(0)
              .please();
    }

    @Test
    void rejectedWhenFeasible() {
      questionEvaluation.feasibleQuestion = true;
      openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

      assertThat(contest().rejected, is(true));
    }

    @Test
    void acceptedWhenNotFeasible() {
      questionEvaluation.feasibleQuestion = false;
      openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

      assertThat(contest().rejected, is(false));
    }

    private QuestionContestResult contest() {
      return aiQuestionGenerator
          .getQuestionContestResult(mcq.getNote(), mcq)
          .getQuestionContestResult(mcq);
    }
  }
}
