package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PredefinedQuestionTest {
  @Autowired MakeMe makeMe;
  @Autowired PredefinedQuestionService predefinedQuestionService;
  @MockitoBean AiQuestionGenerator aiQuestionGenerator;

  User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Nested
  class SpellingQuiz {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().rememberSpelling().please();
      makeMe.aNote("a necessary sibling as filling option").please();
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      PredefinedQuestion randomQuizQuestion = generateQuizQuestionEntity(note);
      assertThat(randomQuizQuestion, instanceOf(PredefinedQuestion.class));
      PredefinedQuestion qq = randomQuizQuestion;
      assertThat(
          qq.getMultipleChoicesQuestion().getQuestionStem(),
          containsString(mcqWithAnswer.getQuestion().getQuestionStem()));
    }
  }

  @Nested
  class AutoEvaluateAndRegenerate {
    Note note;
    MCQWithAnswer mcqWithAnswer;
    QuestionEvaluation contestResult;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      contestResult = new QuestionEvaluation();
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationPassesOrFails() {
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      contestResult.feasibleQuestion = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void storesSameContextSeedOnPredefinedQuestionAsPassedToAiGenerator() {
      contestResult.feasibleQuestion = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);

      ArgumentCaptor<Long> seedCaptor = ArgumentCaptor.forClass(Long.class);
      Mockito.reset(aiQuestionGenerator);
      when(aiQuestionGenerator.getAiGeneratedQuestion(
              eq(note), isNull(), seedCaptor.capture(), any()))
          .thenReturn(mcqWithAnswer);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getContextSeed(), equalTo(seedCaptor.getValue()));
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationApiFails() {
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      // Simulate evaluation API failure by returning null
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(null);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      // Should still return the generated question even when evaluation fails
      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void shouldRegenerateQuestionWhenEvaluationShowsNotFeasible() {
      MCQWithAnswer regeneratedQuestion = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      contestResult.feasibleQuestion = true;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any(), any(), any()))
          .thenReturn(regeneratedQuestion);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(regeneratedQuestion));
    }
  }

  private PredefinedQuestion generateQuizQuestionEntity(@NotNull Note note) {
    return predefinedQuestionService.generateAFeasibleQuestion(note);
  }
}
