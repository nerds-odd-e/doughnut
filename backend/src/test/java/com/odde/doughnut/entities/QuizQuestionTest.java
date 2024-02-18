package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionAIQuestion;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionClozeSelection;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionSpelling;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.AiQuestionGeneratorForNote;
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
class QuizQuestionTest {
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
    makeMe.refresh(top);
    QuizQuestion quizQuestion = generateQuizQuestion(note);
    assertThat(
        quizQuestion.getStem(),
        containsString(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {
    private List<String> getOptions(Note note) {
      QuizQuestion quizQuestion = generateQuizQuestion(note);
      return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
    }

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
        makeMe.refresh(top);
      }

      @Test
      void descendingRandomizer() {
        List<String> options = getOptions(note1);
        assertThat(
            options,
            containsInRelativeOrder(note2.getTopicConstructor(), note1.getTopicConstructor()));
      }

      @Test
      void ascendingRandomizer() {
        randomizer.alwaysChoose = "last";
        List<String> options = getOptions(note1);
        assertThat(
            options,
            containsInRelativeOrder(note1.getTopicConstructor(), note2.getTopicConstructor()));
      }
    }

    @Test
    void aNoteWithManySiblings() {
      Note top = makeMe.aNote().please();
      makeMe.theNote(top).with10Children().please();
      Note note = makeMe.aNote().under(top).please();
      makeMe.refresh(top);
      List<String> options = getOptions(note);
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
      makeMe.refresh(top);
    }

    @Test
    void typeShouldBeSpellingQuiz() {
      assertThat(generateQuizQuestionEntity(note), instanceOf(QuizQuestionSpelling.class));
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      userModel.getEntity().setAiQuestionTypeOnlyForReview(true);
      AiQuestionGenerator questionGenerator = mock(AiQuestionGenerator.class);
      when(questionGenerator.getAiGeneratedQuestion(any())).thenReturn(mcqWithAnswer);
      QuizQuestionEntity randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), questionGenerator);
      assertThat(randomQuizQuestion, instanceOf(QuizQuestionAIQuestion.class));
      QuizQuestion qq = makeMe.modelFactoryService.toQuizQuestion(randomQuizQuestion);
      assertThat(qq.stem, containsString(mcqWithAnswer.stem));
    }

    @Test
    void shouldReturnTheSameType() {
      QuizQuestionEntity randomQuizQuestion =
          generateQuizQuestion(note, new RealRandomizer(), null);
      Set<Class<? extends QuizQuestionEntity>> types = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        types.add(randomQuizQuestion.getClass());
      }
      assertThat(types, hasSize(1));
    }

    @Test
    void shouldChooseTypeRandomly() {
      Set<Class<? extends QuizQuestionEntity>> types = new HashSet<>();
      for (int i = 0; i < 10; i++) {
        QuizQuestionEntity randomQuizQuestion =
            generateQuizQuestion(note, new RealRandomizer(),null);
        types.add(randomQuizQuestion.getClass());
      }
      assertThat(
          types, containsInAnyOrder(QuizQuestionSpelling.class, QuizQuestionClozeSelection.class));
    }
  }

  private QuizQuestionEntity generateQuizQuestion(
    Note note, Randomizer randomizer1, AiQuestionGenerator aiQuestionGenerator) {
    QuizQuestionGenerator quizQuestionGenerator =
        new QuizQuestionGenerator(
            userModel.getEntity(), note, randomizer1, makeMe.modelFactoryService);
    return quizQuestionGenerator.generateAQuestionOfRandomType(aiQuestionGenerator);
  }

  private QuizQuestionEntity generateQuizQuestionEntity(Note note) {
    return generateQuizQuestion(note, randomizer,null);
  }

  private QuizQuestion generateQuizQuestion(Note note) {
    QuizQuestionEntity entity = generateQuizQuestionEntity(note);
    return makeMe.modelFactoryService.toQuizQuestion(entity);
  }
}
