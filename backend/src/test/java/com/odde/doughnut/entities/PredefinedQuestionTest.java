package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PredefinedQuestionTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  AiQuestionGenerator aiQuestionGenerator = mock(AiQuestionGenerator.class);

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
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
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any())).thenReturn(mcqWithAnswer);
      PredefinedQuestion randomQuizQuestion = generateQuizQuestionEntity(note);
      assertThat(randomQuizQuestion, instanceOf(PredefinedQuestion.class));
      PredefinedQuestion qq = randomQuizQuestion;
      assertThat(
          qq.getMultipleChoicesQuestion().getStem(),
          containsString(mcqWithAnswer.getMultipleChoicesQuestion().getStem()));
    }
  }

  @Nested
  class AutoEvaluateAndRegenerate {
    Note note;
    MCQWithAnswer mcqWithAnswer;
    QuestionContestResult contestResult;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().please();
      mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      contestResult = new QuestionContestResult();
      contestResult.advice = "This question needs improvement";
    }

    @Test
    void shouldReturnOriginalQuestionWhenEvaluationPassesOrFails() {
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any())).thenReturn(mcqWithAnswer);
      contestResult.rejected = true;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);

      PredefinedQuestionService service =
          new PredefinedQuestionService(makeMe.modelFactoryService, aiQuestionGenerator);
      PredefinedQuestion result = service.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void shouldRegenerateQuestionWhenEvaluationShowsNotFeasible() throws JsonProcessingException {
      MCQWithAnswer regeneratedQuestion = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any())).thenReturn(mcqWithAnswer);
      contestResult.rejected = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any()))
          .thenReturn(regeneratedQuestion);

      PredefinedQuestionService service =
          new PredefinedQuestionService(makeMe.modelFactoryService, aiQuestionGenerator);
      PredefinedQuestion result = service.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(regeneratedQuestion));
    }

    @Test
    void shouldUseOriginalQuestionWhenRegenerationFails() throws JsonProcessingException {
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any())).thenReturn(mcqWithAnswer);
      contestResult.rejected = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any()))
          .thenThrow(new JsonProcessingException("Error") {});

      PredefinedQuestionService service =
          new PredefinedQuestionService(makeMe.modelFactoryService, aiQuestionGenerator);
      PredefinedQuestion result = service.generateAFeasibleQuestion(note);

      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void shouldSaveBothOriginalAndRegeneratedQuestions() throws JsonProcessingException {
      // Setup
      MCQWithAnswer regeneratedQuestion = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any(), any())).thenReturn(mcqWithAnswer);
      contestResult.rejected = false;
      when(aiQuestionGenerator.getQuestionContestResult(any(), any())).thenReturn(contestResult);
      when(aiQuestionGenerator.regenerateQuestion(any(), any(), any()))
          .thenReturn(regeneratedQuestion);

      // Use ArgumentCaptor to capture questions being saved
      ModelFactoryService mockModelFactory = mock(ModelFactoryService.class);
      ArgumentCaptor<PredefinedQuestion> questionCaptor =
          ArgumentCaptor.forClass(PredefinedQuestion.class);
      when(mockModelFactory.save(questionCaptor.capture())).thenAnswer(i -> i.getArgument(0));

      // Execute
      PredefinedQuestionService service =
          new PredefinedQuestionService(mockModelFactory, aiQuestionGenerator);
      PredefinedQuestion result = service.generateAFeasibleQuestion(note);

      // Verify
      // Should have captured two questions (original and regenerated)
      assertThat(questionCaptor.getAllValues().size(), equalTo(2));

      // The first saved question should be the original with contested=true
      PredefinedQuestion firstSavedQuestion = questionCaptor.getAllValues().get(0);
      assertThat(
          firstSavedQuestion.getMcqWithAnswer().getMultipleChoicesQuestion(),
          equalTo(mcqWithAnswer.getMultipleChoicesQuestion()));
      assertThat(
          "First question should be marked as contested",
          firstSavedQuestion.isContested(),
          is(true));

      // The second saved question should be the regenerated one
      PredefinedQuestion secondSavedQuestion = questionCaptor.getAllValues().get(1);
      assertThat(
          secondSavedQuestion.getMcqWithAnswer().getMultipleChoicesQuestion(),
          equalTo(regeneratedQuestion.getMultipleChoicesQuestion()));
      assertThat(secondSavedQuestion.isContested(), is(false));

      // The result should be the regenerated question
      assertThat(
          result.getMcqWithAnswer().getMultipleChoicesQuestion(),
          equalTo(regeneratedQuestion.getMultipleChoicesQuestion()));
    }
  }

  private PredefinedQuestion generateQuizQuestionEntity(@NotNull Note note) {
    PredefinedQuestionService predefinedQuestionService =
        new PredefinedQuestionService(makeMe.modelFactoryService, aiQuestionGenerator);
    return predefinedQuestionService.generateAFeasibleQuestion(note);
  }
}
