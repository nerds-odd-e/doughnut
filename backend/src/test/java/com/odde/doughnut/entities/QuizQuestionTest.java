package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.models.randomizers.NonRandomizer;
import com.odde.doughnut.models.randomizers.RealRandomizer;
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
        getQuizQuestion(note).getQuestionType(), equalTo(QuizQuestion.QuestionType.JUST_REVIEW));
  }

  @Test
  void useClozeDescription() {
    Note top = makeMe.aHeadNote().please();
    makeMe.aNote().under(top).please();
    Note note = makeMe.aNote().under(top).title("abc").description("abc has 3 letters").please();
    makeMe.refresh(top);
    QuizQuestionViewedByUser quizQuestion = getQuizQuestion(note);
    assertThat(
        quizQuestion.getDescription(),
        equalTo(
            "<mark title='Hidden text that is matching the answer'>[...]</mark> has 3 letters"));
  }

  @Nested
  class ClozeSelectionQuiz {
    private List<String> getOptions(Note note) {
      QuizQuestionViewedByUser quizQuestion = getQuizQuestion(note);
      return quizQuestion.getOptions().stream()
          .map(QuizQuestionViewedByUser.Option::getDisplay)
          .toList();
    }

    @Test
    void aNoteWithNoSiblingsShouldDoJustReview() {
      Note note = makeMe.aHeadNote().please();
      QuizQuestionViewedByUser quizQuestion = getQuizQuestion(note);
      assertThat(quizQuestion.getQuestionType(), equalTo(QuizQuestion.QuestionType.JUST_REVIEW));
    }

    @Test
    void aNoteWithOneSibling() {
      Note top = makeMe.aHeadNote().please();
      Note note1 = makeMe.aNote().under(top).please();
      Note note2 = makeMe.aNote().under(top).please();
      makeMe.refresh(top);
      List<String> options = getOptions(note1);
      assertThat(options, containsInAnyOrder(note1.getTitle(), note2.getTitle()));
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
          getQuizQuestion(note).getQuestionType(), equalTo(QuizQuestion.QuestionType.SPELLING));
    }

    @Test
    void shouldReturnTheSameType() {
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      QuizQuestion randomQuizQuestion = reviewPoint.generateAQuizQuestion(new RealRandomizer());
      Set<QuizQuestion.QuestionType> types = new HashSet<>();
      for (int i = 0; i < 3; i++) {
        types.add(randomQuizQuestion.getQuestionType());
      }
      assertThat(types, hasSize(1));
    }

    @Test
    void shouldChooseTypeRandomly() {
      Set<QuizQuestion.QuestionType> types = new HashSet<>();
      ReviewPointModel reviewPoint = getReviewPointModel(note);
      for (int i = 0; i < 10; i++) {
        QuizQuestion randomQuizQuestion = reviewPoint.generateAQuizQuestion(new RealRandomizer());
        types.add(randomQuizQuestion.getQuestionType());
      }
      assertThat(
          types,
          containsInAnyOrder(
              QuizQuestion.QuestionType.SPELLING, QuizQuestion.QuestionType.CLOZE_SELECTION));
    }
  }

  private QuizQuestionViewedByUser getQuizQuestion(Note note) {
    return new QuizQuestionViewedByUser(
        getReviewPointModel(note).generateAQuizQuestion(randomizer),
        makeMe.modelFactoryService,
        userModel.getEntity());
  }

  private ReviewPointModel getReviewPointModel(Note note) {
    return makeMe.aReviewPointFor(note).by(userModel).toModelPlease();
  }
}
