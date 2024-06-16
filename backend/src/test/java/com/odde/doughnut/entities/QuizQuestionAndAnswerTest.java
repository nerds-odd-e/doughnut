package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
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
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizQuestionAndAnswerTest {
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

    assertThrows(ResponseStatusException.class, () -> generateQuizQuestion(note));
  }

  @Test
  void useClozeDescription() {
    Note top = makeMe.aNote().please();
    makeMe.aNote().under(top).please();
    Note note =
        makeMe.aNote().under(top).titleConstructor("abc").details("abc has 3 letters").please();
    QuizQuestionAndAnswer quizQuestionAndAnswer = generateQuizQuestion(note);
    assertThat(
        quizQuestionAndAnswer.getQuizQuestion().getMultipleChoicesQuestion().getStem(),
        containsString(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {

    @Test
    void aNoteWithNoSiblingsShouldNotGenerateAnyQuestion() {
      Note note = makeMe.aNote().please();
      assertThrows(ResponseStatusException.class, () -> generateQuizQuestion(note));
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
        QuizQuestionAndAnswer quizQuestionAndAnswer = generateQuizQuestion(note1);
        List<String> options =
            quizQuestionAndAnswer.getQuizQuestion().getMultipleChoicesQuestion().getChoices();
        assertThat(
            options,
            containsInRelativeOrder(note2.getTopicConstructor(), note1.getTopicConstructor()));
      }

      @Test
      void ascendingRandomizer() {
        randomizer.alwaysChoose = "last";
        QuizQuestionAndAnswer quizQuestionAndAnswer = generateQuizQuestion(note1);
        List<String> options =
            quizQuestionAndAnswer.getQuizQuestion().getMultipleChoicesQuestion().getChoices();
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
      QuizQuestionAndAnswer quizQuestionAndAnswer = generateQuizQuestion(note);
      List<String> options =
          quizQuestionAndAnswer.getQuizQuestion().getMultipleChoicesQuestion().getChoices();
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
      assertTrue(generateQuizQuestionEntity(note).getQuizQuestion().getCheckSpell());
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      userModel.getEntity().setAiQuestionTypeOnlyForReview(true);
      AiQuestionGenerator questionGenerator = mock(AiQuestionGenerator.class);
      when(questionGenerator.getAiGeneratedQuestion(any())).thenReturn(mcqWithAnswer);
      QuizQuestionAndAnswer randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), questionGenerator);
      assertThat(randomQuizQuestion, instanceOf(QuizQuestionAndAnswer.class));
      QuizQuestionAndAnswer qq = randomQuizQuestion;
      assertThat(
          qq.getQuizQuestion().getMultipleChoicesQuestion().getStem(),
          containsString(mcqWithAnswer.getMultipleChoicesQuestion().getStem()));
    }

    @Test
    void shouldReturnTheSameType() {
      QuizQuestionAndAnswer randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), null);
      Set<Class<? extends QuizQuestionAndAnswer>> types = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        types.add(randomQuizQuestion.getClass());
      }
      assertThat(types, hasSize(1));
    }

    @Test
    void shouldChooseTypeRandomly() {
      int spellingCount = 0;
      for (int i = 0; i < 20; i++) {
        QuizQuestionAndAnswer randomQuizQuestion =
            generateQuizQuestion(note, new RealRandomizer(), null);
        if (randomQuizQuestion.getQuizQuestion().getCheckSpell() != null
            && randomQuizQuestion.getQuizQuestion().getCheckSpell()) {
          spellingCount++;
        }
      }
      assertThat(spellingCount, greaterThan(0));
      assertThat(spellingCount, lessThan(20));
    }
  }

  private QuizQuestionAndAnswer generateQuizQuestion(
      Note note, Randomizer randomizer1, AiQuestionGenerator aiQuestionGenerator) {
    QuizQuestionGenerator quizQuestionGenerator =
        new QuizQuestionGenerator(
            userModel.getEntity(), note, randomizer1, makeMe.modelFactoryService);
    return quizQuestionGenerator.generateAQuestionOfRandomType(aiQuestionGenerator);
  }

  private QuizQuestionAndAnswer generateQuizQuestionEntity(Note note) {
    return generateQuizQuestion(note, randomizer, null);
  }

  private QuizQuestionAndAnswer generateQuizQuestion(Note note) {
    return generateQuizQuestionEntity(note);
  }
}
