package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionGenerator;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import java.util.HashSet;
import java.util.List;
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

  @Test
  void useClozeDescription() {
    Note top = makeMe.aNote().please();
    makeMe.aNote().under(top).please();
    Note note =
        makeMe.aNote().under(top).titleConstructor("abc").details("abc has 3 letters").please();
    PredefinedQuestion predefinedQuestion = generateQuizQuestion(note);
    assertThat(
        predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
        containsString(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {

    @Test
    void aNoteWithNoSiblingsShouldNotGenerateAnyQuestion() {
      Note note = makeMe.aNote().please();
      assertNull(generateQuizQuestion(note));
    }

    @Nested
    class aNoteWithOneSibling {
      Note note1;
      Note note2;

      @BeforeEach
      void setup() {
        Note top = makeMe.aNote().please();
        note1 = makeMe.aNote().under(top).please();
        note2 = makeMe.aNote().under(top).please();
      }

      @Test
      void descendingRandomizer() {
        PredefinedQuestion predefinedQuestion = generateQuizQuestion(note1);
        List<String> options =
            predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
        assertThat(
            options,
            containsInRelativeOrder(note2.getTopicConstructor(), note1.getTopicConstructor()));
      }

      @Test
      void ascendingRandomizer() {
        randomizer.alwaysChoose = Randomization.RandomStrategy.last;
        PredefinedQuestion predefinedQuestion = generateQuizQuestion(note1);
        List<String> options =
            predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
        assertThat(
            options,
            containsInRelativeOrder(note1.getTopicConstructor(), note2.getTopicConstructor()));
      }
    }

    @Test
    void aNoteWithManySiblings() {
      Note top = makeMe.aNote().please();
      makeMe.theNote(top).withNChildren(10).please();
      Note note = makeMe.aNote().under(top).please();
      PredefinedQuestion predefinedQuestion = generateQuizQuestion(note);
      List<String> options =
          predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
      assertThat(options.size(), equalTo(3));
      assertThat(options.contains(note.getTopicConstructor()), is(true));
    }
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

    @Test
    void shouldChooseTypeRandomly() {
      int spellingCount = 0;
      for (int i = 0; i < 20; i++) {
        PredefinedQuestion randomQuizQuestion =
            generateQuizQuestion(note, new RealRandomizer(), null);
        if (randomQuizQuestion.getBareQuestion().getCheckSpell() != null) {
          if (randomQuizQuestion.getBareQuestion().getCheckSpell()) {
            spellingCount++;
          }
        }
      }
      assertThat(spellingCount, greaterThan(0));
      assertThat(spellingCount, lessThan(20));
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
