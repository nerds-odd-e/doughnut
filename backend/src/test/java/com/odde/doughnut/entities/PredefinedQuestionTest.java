package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionGenerator;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import java.util.HashSet;
import java.util.Set;
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

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
  }

  @Test
  void aNoteWithNoDescriptionHasNoQuiz() {
    Note note = makeMe.aNote().withNoDescription().creatorAndOwner(userModel).please();

    assertNull(generateQuizQuestion(note));
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
      userModel.getEntity().setAiQuestionTypeOnlyForReview(true);
      AiQuestionGenerator questionGenerator = mock(AiQuestionGenerator.class);
      when(questionGenerator.getAiGeneratedQuestion(any())).thenReturn(mcqWithAnswer);
      PredefinedQuestion randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), questionGenerator);
      assertThat(randomQuizQuestion, instanceOf(PredefinedQuestion.class));
      PredefinedQuestion qq = randomQuizQuestion;
      assertThat(
          qq.getBareQuestion().getMultipleChoicesQuestion().getStem(),
          containsString(mcqWithAnswer.getMultipleChoicesQuestion().getStem()));
    }

    @Test
    void shouldReturnTheSameType() {
      PredefinedQuestion randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), null);
      Set<Class<? extends PredefinedQuestion>> types = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        types.add(randomQuizQuestion.getClass());
      }
      assertThat(types, hasSize(1));
    }
  }

  private PredefinedQuestion generateQuizQuestion(
      Note note, Randomizer randomizer1, AiQuestionGenerator aiQuestionGenerator) {
    PredefinedQuestionGenerator predefinedQuestionGenerator =
        new PredefinedQuestionGenerator(
            userModel.getEntity(), note, randomizer1, makeMe.modelFactoryService);
    return predefinedQuestionGenerator.generateAQuestionOfRandomType(aiQuestionGenerator);
  }

  private PredefinedQuestion generateQuizQuestionEntity(Note note) {
    return generateQuizQuestion(note, randomizer, null);
  }

  private PredefinedQuestion generateQuizQuestion(Note note) {
    return generateQuizQuestionEntity(note);
  }
}
