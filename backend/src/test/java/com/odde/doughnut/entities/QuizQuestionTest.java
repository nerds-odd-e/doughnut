package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.testability.MakeMe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
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
    assertThat(
        getQuizQuestion(note).getQuestionType(),
        equalTo(QuizQuestionEntity.QuestionType.JUST_REVIEW));
  }

  @Test
  void useClozeDescription() {
    Note top = makeMe.aHeadNote().please();
    makeMe.aNote().under(top).please();
    Note note = makeMe.aNote().under(top).title("abc").description("abc has 3 letters").please();
    makeMe.refresh(top);
    QuizQuestion quizQuestion = getQuizQuestion(note);
    assertThat(
        quizQuestion.getStem(),
        equalTo(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {
    private List<String> getOptions(Note note) {
      QuizQuestion quizQuestion = getQuizQuestion(note);
      return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
    }

    @Test
    void aNoteWithNoSiblingsShouldDoJustReview() {
      Note note = makeMe.aHeadNote().please();
      QuizQuestion quizQuestion = getQuizQuestion(note);
      assertThat(
          quizQuestion.getQuestionType(), equalTo(QuizQuestionEntity.QuestionType.JUST_REVIEW));
    }

    @Nested
    class aNoteWithOneSibling {
      Note note1;
      Note note2;

      @BeforeEach
      void setup() {
        Note top = makeMe.aHeadNote().please();
        note1 = makeMe.aNote().under(top).please();
        note2 = makeMe.aNote().under(top).please();
        makeMe.refresh(top);
      }

      @Test
      void descendingRandomizer() {
        List<String> options = getOptions(note1);
        assertThat(options, containsInRelativeOrder(note2.getTitle(), note1.getTitle()));
      }

      @Test
      void ascendingRandomizer() {
        randomizer.alwaysChoose = "last";
        List<String> options = getOptions(note1);
        assertThat(options, containsInRelativeOrder(note1.getTitle(), note2.getTitle()));
      }
    }

    @Test
    void aNoteWithManySiblings() {
      Note top = makeMe.aHeadNote().please();
      makeMe.theNote(top).with10Children().please();
      Note note = makeMe.aNote().under(top).please();
      makeMe.refresh(top);
      List<String> options = getOptions(note);
      assertThat(options.size(), equalTo(3));
      assertThat(options.contains(note.getTitle()), is(true));
    }
  }

  @Nested
  class SpellingQuiz {
    Note note;

    @BeforeEach
    void setup() {
      Note top = makeMe.aHeadNote().please();
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
      AIGeneratedQuestion aiGeneratedQuestion = new AIGeneratedQuestion();
      aiGeneratedQuestion.stem = "wat is the meaning of life?";
      userModel.getEntity().setAiQuestionTypeOnlyForReview(true);
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      AiAdvisorService aiAdvisorService = mock(AiAdvisorService.class);
      when(aiAdvisorService.generateQuestion(any())).thenReturn(aiGeneratedQuestion);
      QuizQuestionEntity randomQuizQuestion =
          reviewPoint.generateAQuizQuestion(
              new RealRandomizer(), userModel.getEntity(), aiAdvisorService);
      assertThat(
          randomQuizQuestion.getQuestionType(),
          equalTo(QuizQuestionEntity.QuestionType.AI_QUESTION));
      assertThat(randomQuizQuestion.getRawJsonQuestion(), containsString("wat"));
    }

    @Test
    void shouldReturnTheSameType() {
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      QuizQuestionEntity randomQuizQuestion =
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
        QuizQuestionEntity randomQuizQuestion =
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
    QuizQuestionEntity quizQuestion =
        getReviewPointModel(note).generateAQuizQuestion(randomizer, userModel.getEntity(), null);
    return makeMe.modelFactoryService.toQuizQuestion(quizQuestion, userModel.getEntity());
  }

  private ReviewPointModel getReviewPointModel(Note note) {
    return makeMe.aReviewPointFor(note).by(userModel).toModelPlease();
  }
}
