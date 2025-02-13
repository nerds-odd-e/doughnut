package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

  private PredefinedQuestion generateQuizQuestionEntity(@NotNull Note note) {
    PredefinedQuestionService predefinedQuestionService =
        new PredefinedQuestionService(makeMe.modelFactoryService, aiQuestionGenerator);
    return predefinedQuestionService.generateAFeasibleQuestion(note);
  }
}
