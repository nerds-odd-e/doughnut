package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.AiAdvisorService;
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

    ReviewPointModel reviewPointModel = getReviewPointModel(note);
    assertThrows(
        ResponseStatusException.class,
        () -> reviewPointModel.generateAQuizQuestion(randomizer, userModel.getEntity(), null));
  }

  @Test
  void useClozeDescription() {
    Note top = makeMe.aNote().please();
    makeMe.aNote().under(top).please();
    Note note = makeMe.aNote().under(top).title("abc").details("abc has 3 letters").please();
    makeMe.refresh(top);
    QuizQuestion quizQuestion = getQuizQuestion(note);
    assertThat(
        quizQuestion.getStem(),
        containsString(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {
    private List<String> getOptions(Note note) {
      QuizQuestion quizQuestion = getQuizQuestion(note);
      return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
    }

    @Test
    void aNoteWithNoSiblingsShouldNotGenerateAnyQuestion() {
      Note note = makeMe.aNote().please();
      ReviewPointModel reviewPointModel = getReviewPointModel(note);
      assertThrows(
          ResponseStatusException.class,
          () -> reviewPointModel.generateAQuizQuestion(randomizer, userModel.getEntity(), null));
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
        assertThat(options, containsInRelativeOrder(note2.getTopic(), note1.getTopic()));
      }

      @Test
      void ascendingRandomizer() {
        randomizer.alwaysChoose = "last";
        List<String> options = getOptions(note1);
        assertThat(options, containsInRelativeOrder(note1.getTopic(), note2.getTopic()));
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
      assertThat(options.contains(note.getTopic()), is(true));
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
      assertThat(
          getQuizQuestion(note).getQuestionType(),
          equalTo(QuizQuestionEntity.QuestionType.SPELLING));
    }

    @Test
    void shouldAlwaysChooseAIQuestionIfConfigured() throws QuizQuestionNotPossibleException {
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      userModel.getEntity().setAiQuestionTypeOnlyForReview(true);
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      AiAdvisorService aiAdvisorService = mock(AiAdvisorService.class);
      when(aiAdvisorService.generateQuestion(any(), any())).thenReturn(mcqWithAnswer);
      QuizQuestion randomQuizQuestion =
          reviewPoint.generateAQuizQuestion(
              new RealRandomizer(), userModel.getEntity(), aiAdvisorService);
      assertThat(
          randomQuizQuestion.getQuestionType(),
          equalTo(QuizQuestionEntity.QuestionType.AI_QUESTION));
      assertThat(randomQuizQuestion.stem, containsString(mcqWithAnswer.stem));
    }

    @Test
    void shouldReturnTheSameType() {
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      QuizQuestion randomQuizQuestion =
          reviewPoint.generateAQuizQuestion(new RealRandomizer(), userModel.getEntity(), null);
      Set<QuizQuestionEntity.QuestionType> types = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        types.add(randomQuizQuestion.getQuestionType());
      }
      assertThat(types, hasSize(1));
    }

    @Test
    void shouldChooseTypeRandomly() {
      Set<QuizQuestionEntity.QuestionType> types = new HashSet<>();
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      for (int i = 0; i < 10; i++) {
        QuizQuestion randomQuizQuestion =
            reviewPoint.generateAQuizQuestion(new RealRandomizer(), userModel.getEntity(), null);
        types.add(randomQuizQuestion.getQuestionType());
      }
      assertThat(
          types,
          containsInAnyOrder(
              QuizQuestionEntity.QuestionType.SPELLING,
              QuizQuestionEntity.QuestionType.CLOZE_SELECTION));
    }
  }

  private QuizQuestion getQuizQuestion(Note note) {
    ReviewPointModel reviewPointModel = getReviewPointModel(note);
    return reviewPointModel.generateAQuizQuestion(randomizer, userModel.getEntity(), null);
  }

  private ReviewPointModel getReviewPointModel(Note note) {
    return makeMe.aReviewPointFor(note).by(userModel).toModelPlease();
  }
}
