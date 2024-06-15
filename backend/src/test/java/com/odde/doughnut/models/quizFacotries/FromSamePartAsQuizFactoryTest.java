package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.factories.FromSamePartAsQuizFactory;
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
  Note subjectivePerspective;
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
    subjectivePerspective = makeMe.aLink().between(subjective, perspective, LinkType.PART).please();
    makeMe.aLink().between(objective, perspective, LinkType.PART).please();
    Note uglySubjective = makeMe.aLink().between(ugly, subjective, LinkType.TAGGED_BY).please();
    reviewPoint = makeMe.aReviewPointFor(uglySubjective).by(userModel).inMemoryPlease();
  }

  @Test
  void shouldBeInvalidWhenNoCousin() {
    assertThat(buildQuestion(), nullValue());
  }

  @Nested
  class WhenThereIsAnCousin {
    Note cousin;

    @BeforeEach
    void setup() {
      cousin = makeMe.aLink().between(pretty, subjective, LinkType.TAGGED_BY).please();
    }

    @Test
    void shouldBeInvalidWhenNoFillingOptions() {
      assertThat(buildQuestion(), nullValue());
    }

    @Nested
    class WhenThereIsFillingChoice {

      @BeforeEach
      void setup() {
        makeMe.aLink().between(tall, objective, LinkType.TAGGED_BY).please();
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
        }

        @Test
        void shouldIncludeRightAnswersAndFillingOptions() {
          QuizQuestion quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getMultipleChoicesQuestion().getStem(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> the same part of <mark>perspective</mark> as:"));
          assertThat(
              quizQuestion.getMultipleChoicesQuestion().getStem(),
              containsString(ugly.getTopicConstructor()));
          List<String> strings = quizQuestion.getMultipleChoicesQuestion().getChoices();
          assertThat(pretty.getTopicConstructor(), in(strings));
          assertThat(tall.getTopicConstructor(), in(strings));
          assertThat(ugly.getTopicConstructor(), not(in(strings)));
        }
      }
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(
        new FromSamePartAsQuizFactory((LinkingNote) reviewPoint.getNote(), null), reviewPoint);
  }
}
