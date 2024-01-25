package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.FROM_SAME_PART_AS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.Link;
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
class FromSamePartAsQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note perspective;
  Note subjective;
  Note objective;
  Note ugly;
  Note pretty;
  Note tall;
  Link subjectivePerspective;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote("top").creatorAndOwner(userModel).please();
    perspective = makeMe.aNote("perspective").under(top).please();
    subjective = makeMe.aNote("subjective").under(top).please();
    objective = makeMe.aNote("objective").under(top).please();
    ugly = makeMe.aNote("ugly").under(top).please();
    pretty = makeMe.aNote("pretty").under(top).please();
    tall = makeMe.aNote("tall").under(top).please();
    subjectivePerspective =
        makeMe.aLink().between(subjective, perspective, Link.LinkType.PART).please();
    makeMe.aLink().between(objective, perspective, Link.LinkType.PART).please();
    Link uglySubjective =
        makeMe.aLink().between(ugly, subjective, Link.LinkType.TAGGED_BY).please();
    reviewPoint = makeMe.aReviewPointFor(uglySubjective).by(userModel).inMemoryPlease();
    makeMe.refresh(top);
  }

  @Test
  void shouldBeInvalidWhenNoCousin() {
    assertThat(buildQuestion(), nullValue());
  }

  @Nested
  class WhenThereIsAnCousin {
    Link cousin;

    @BeforeEach
    void setup() {
      cousin = makeMe.aLink().between(pretty, subjective, Link.LinkType.TAGGED_BY).please();
      makeMe.refresh(userModel.getEntity());
    }

    @Test
    void shouldBeInvalidWhenNoFillingOptions() {
      assertThat(buildQuestion(), nullValue());
    }

    @Nested
    class WhenThereIsFillingChoice {

      @BeforeEach
      void setup() {
        makeMe.aLink().between(tall, objective, Link.LinkType.TAGGED_BY).please();
        makeMe.refresh(userModel.getEntity());
      }

      @Test
      void shouldBeInvalidWhenNoViceReviewPoint() {
        assertThat(buildQuestion(), nullValue());
      }

      @Nested
      class WhenThereIsViceReviewPoint {
        @BeforeEach
        void setup() {
          makeMe.aReviewPointFor(cousin).by(userModel).please();
          makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswersAndFillingOptions() {
          QuizQuestion quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getStem(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> the same part of <mark>perspective</mark> as:"));
          assertThat(quizQuestion.getMainTopic(), containsString(ugly.getTopicConstructor()));
          List<String> strings = toOptionStrings(quizQuestion);
          assertThat(pretty.getTopicConstructor(), in(strings));
          assertThat(tall.getTopicConstructor(), in(strings));
          assertThat(ugly.getTopicConstructor(), not(in(strings)));
        }
      }
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(FROM_SAME_PART_AS, reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    List<QuizQuestion.Choice> choices = quizQuestion.getChoices();
    return choices.stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}
