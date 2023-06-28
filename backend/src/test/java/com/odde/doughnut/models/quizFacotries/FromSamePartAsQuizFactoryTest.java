package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.FROM_SAME_PART_AS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
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
    class WhenThereIsFillingOption {

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
          QuizQuestionViewedByUser quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getDescription(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> the same part of <mark>perspective</mark> as:"));
          assertThat(quizQuestion.getMainTopic(), containsString(ugly.getTitle()));
          List<String> strings = toOptionStrings(quizQuestion);
          assertThat(pretty.getTitle(), in(strings));
          assertThat(tall.getTitle(), in(strings));
          assertThat(ugly.getTitle(), not(in(strings)));
        }

        @Nested
        class WhenThereIsReviewPointOfTheCategory {
          ReviewPoint additionalReviewPoint;

          @BeforeEach
          void setup() {
            additionalReviewPoint =
                makeMe.aReviewPointFor(subjectivePerspective).by(userModel).please();
          }

          @Test
          void shouldInclude2ViceReviewPoints() {
            QuizQuestionViewedByUser quizQuestion = buildQuestion();
            List<Integer> viceReviewPointIds = quizQuestion.getViceReviewPointIdList();
            assertThat(viceReviewPointIds, hasSize(2));
            assertThat(additionalReviewPoint.getId(), in(viceReviewPointIds));
          }
        }

        @Nested
        class Answer {
          @Test
          void correct() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(reviewPoint)
                    .validQuestionOfType(FROM_SAME_PART_AS)
                    .answerWithSpelling(pretty.getTitle())
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
          }

          @Test
          void wrong() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(reviewPoint)
                    .validQuestionOfType(FROM_SAME_PART_AS)
                    .answerWithSpelling("metal")
                    .inMemoryPlease();
            assertFalse(answerResult.correct);
          }
        }
      }
    }
  }

  private QuizQuestionViewedByUser buildQuestion() {
    return makeMe.buildAQuestion(FROM_SAME_PART_AS, reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestionViewedByUser quizQuestion) {
    List<QuizQuestionViewedByUser.Option> options = quizQuestion.getOptions();
    return options.stream().map(QuizQuestionViewedByUser.Option::getDisplay).toList();
  }
}
