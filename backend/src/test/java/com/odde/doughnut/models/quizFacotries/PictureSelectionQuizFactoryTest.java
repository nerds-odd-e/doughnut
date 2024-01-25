package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.PICTURE_SELECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
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
class PictureSelectionQuizFactoryTest {
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
  void shouldReturnNullIfCannotFindPicture() {
    assertThat(buildQuestion(), is(nullValue()));
  }

  @Nested
  class WhenThereIsPicture {
    @BeforeEach
    void setup() {
      makeMe.theNote(source).pictureUrl("http://img/img.jpg").please();
    }

    @Test
    void shouldReturnNullIfCannotFindPicture() {
      assertThat(buildQuestion(), is(nullValue()));
    }

    @Nested
    class WhenThereIsAnotherPictureNote {
      @BeforeEach
      void setup() {
        makeMe.theNote(brother).pictureUrl("http://img/img2.jpg").please();
      }

      @Test
      void shouldIncludeRightAnswers() {
        QuizQuestion quizQuestion = buildQuestion();
        assertThat(quizQuestion.getStem(), equalTo(""));
        assertThat(quizQuestion.getMainTopic(), equalTo("source"));
        List<String> options = toOptionStrings(quizQuestion);
        assertThat(source.getTopicConstructor(), in(options));
      }
    }

    @Nested
    class WhenThereIsAnotherPictureInUncleNote {
      @BeforeEach
      void setup() {
        makeMe.theNote(uncle).pictureUrl("http://img/img2.jpg").please();
      }

      @Test
      void shouldIncludeUncle() {
        QuizQuestion quizQuestion = buildQuestion();
        List<String> options = toOptionStrings(quizQuestion);
        assertThat(uncle.getTopicConstructor(), in(options));
      }
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(PICTURE_SELECTION, reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    return quizQuestion.getChoices().stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}
