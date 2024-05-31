package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.factories.ImageSelectionQuizFactory;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
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
class ImageSelectionQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note father;
  Note source;
  Note brother;
  Note uncle;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote().creatorAndOwner(userModel).please();
    father = makeMe.aNote("second level").under(top).please();
    uncle = makeMe.aNote("uncle").under(top).please();
    source = makeMe.aNote("source").under(father).please();
    brother = makeMe.aNote("another note").under(father).please();
    reviewPoint = makeMe.aReviewPointFor(source).inMemoryPlease();
    makeMe.refresh(top);
    makeMe.refresh(father);
  }

  @Test
  void shouldReturnNullIfCannotFindImage() {
    assertThat(buildQuestion(), is(nullValue()));
  }

  @Nested
  class WhenThereIsImage {
    @BeforeEach
    void setup() {
      makeMe.theNote(source).imageUrl("http://img/img.jpg").please();
    }

    @Test
    void shouldReturnNullIfCannotFindImage() {
      assertThat(buildQuestion(), is(nullValue()));
    }

    @Nested
    class WhenThereIsAnotherImageNote {
      @BeforeEach
      void setup() {
        makeMe.theNote(brother).imageUrl("http://img/img2.jpg").please();
      }

      @Test
      void shouldIncludeRightAnswers() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion.getStem(), containsString("source"));
        List<String> options = toOptionStrings(quizQuestion);
        assertThat(source.getTopicConstructor(), in(options));
      }
    }

    @Nested
    class WhenThereIsAnotherImageInUncleNote {
      @BeforeEach
      void setup() {
        makeMe.theNote(uncle).imageUrl("http://img/img2.jpg").please();
      }

      @Test
      void shouldIncludeUncle() {
        QuizQuestion quizQuestion = buildQuestion();
        List<String> options = toOptionStrings(quizQuestion);
        assertThat(uncle.getTopicConstructor(), in(options));
      }

      @Test
      void shouldNotIncludeUncleIfEnoughOptions() {
        makeMe.theNote(brother).imageUrl("http://img/img2.jpg").please();
        QuizQuestion quizQuestion = buildQuestion();
        List<String> options = toOptionStrings(quizQuestion);
        assertThat(uncle.getTopicConstructor(), not(in(options)));
      }
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(new ImageSelectionQuizFactory(reviewPoint.getNote()), reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}
