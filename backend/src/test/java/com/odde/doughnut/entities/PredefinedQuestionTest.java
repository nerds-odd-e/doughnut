package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.factoryServices.EntityPersister;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PredefinedQuestionTest {
  @Autowired MakeMe makeMe;
  @Autowired EntityPersister entityPersister;
  @Autowired PredefinedQuestionService predefinedQuestionService;
  @MockitoBean AiQuestionGenerator aiQuestionGenerator;

  @Value("${question.regeneration.times:1}")
  int regenerationTimes;

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
      Note top = makeMe.aNote().please();
      note = makeMe.aNote().under(top).rememberSpelling().please();
      makeMe.aNote("a necessary sibling as filling option").under(top).please();
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      PredefinedQuestion randomQuizQuestion = generateQuizQuestionEntity(note);
      assertThat(randomQuizQuestion, instanceOf(PredefinedQuestion.class));
      PredefinedQuestion qq = randomQuizQuestion;
      assertThat(
          qq.getMultipleChoicesQuestion().getF0__stem(),
          containsString(mcqWithAnswer.getF0__multipleChoicesQuestion().getF0__stem()));
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
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      contestResult.feasibleQuestion = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationApiFails() {
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any()))
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
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      contestResult.feasibleQuestion = true;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any()))
          .thenReturn(regeneratedQuestion);

      PredefinedQuestion result = predefinedQuestionService.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(regeneratedQuestion));
    }

    @Test
    void shouldSaveBothOriginalAndRegeneratedQuestions() {
      // Setup
      MCQWithAnswer regeneratedQuestion = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any(), any()))
          .thenReturn(mcqWithAnswer);
      contestResult.feasibleQuestion = true;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any()))
          .thenReturn(regeneratedQuestion);

      // Use ArgumentCaptor to capture questions being saved
      EntityPersister mockEntityPersister = mock(EntityPersister.class);
      ArgumentCaptor<PredefinedQuestion> questionCaptor =
          ArgumentCaptor.forClass(PredefinedQuestion.class);
      when(mockEntityPersister.save(questionCaptor.capture())).thenAnswer(i -> i.getArgument(0));

      // Execute
      PredefinedQuestionService service = createPredefinedQuestionService(mockEntityPersister);
      PredefinedQuestion result = service.generateAFeasibleQuestion(note);

      // Verify
      // Should have captured two questions (original and regenerated)
      assertThat(questionCaptor.getAllValues().size(), equalTo(2));

      // The first saved question should be the original with contested=true
      PredefinedQuestion firstSavedQuestion = questionCaptor.getAllValues().get(0);
      assertThat(
          firstSavedQuestion.getMcqWithAnswer().getF0__multipleChoicesQuestion(),
          equalTo(mcqWithAnswer.getF0__multipleChoicesQuestion()));
      assertThat(
          "First question should be marked as contested",
          firstSavedQuestion.isContested(),
          is(true));

      // The second saved question should be the regenerated one
      PredefinedQuestion secondSavedQuestion = questionCaptor.getAllValues().get(1);
      assertThat(
          secondSavedQuestion.getMcqWithAnswer().getF0__multipleChoicesQuestion(),
          equalTo(regeneratedQuestion.getF0__multipleChoicesQuestion()));
      assertThat(secondSavedQuestion.isContested(), is(false));

      // The result should be the regenerated question
      assertThat(
          result.getMcqWithAnswer().getF0__multipleChoicesQuestion(),
          equalTo(regeneratedQuestion.getF0__multipleChoicesQuestion()));
    }
  }

  private PredefinedQuestion generateQuizQuestionEntity(@NotNull Note note) {
    return predefinedQuestionService.generateAFeasibleQuestion(note);
  }

  private PredefinedQuestionService createPredefinedQuestionService(EntityPersister persister) {
    return new PredefinedQuestionService(persister, aiQuestionGenerator, regenerationTimes);
  }
}
