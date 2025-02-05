package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionGenerator;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
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
  NonRandomizer randomizer = new NonRandomizer();
  AiQuestionGenerator aiQuestionGenerator = mock(AiQuestionGenerator.class);

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
  }

  @Test
  void aNoteWithNoDescriptionHasNoQuiz() {
    Note note = makeMe.aNote().withNoDescription().creatorAndOwner(userModel).please();

    assertNull(generateQuizQuestionEntity(note));
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
    void typeShouldBeSpellingQuiz() {
      PredefinedQuestion question = generateQuizQuestionEntity(note);
      assertTrue(question.getBareQuestion().getCheckSpell());
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      when(aiQuestionGenerator.getAiGeneratedQuestion(any())).thenReturn(mcqWithAnswer);
      PredefinedQuestion randomQuizQuestion = generateQuizQuestionEntity(note);
      assertThat(randomQuizQuestion, instanceOf(PredefinedQuestion.class));
      PredefinedQuestion qq = randomQuizQuestion;
      assertThat(
          qq.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          containsString(mcqWithAnswer.getMultipleChoicesQuestion().getStem()));
    }
  }

  private PredefinedQuestion generateQuizQuestionEntity(Note note) {
    PredefinedQuestionGenerator predefinedQuestionGenerator =
        new PredefinedQuestionGenerator(
            userModel.getEntity(), note, randomizer, makeMe.modelFactoryService);
    return predefinedQuestionGenerator.generateAQuestionOfRandomType(aiQuestionGenerator, null);
  }
}
