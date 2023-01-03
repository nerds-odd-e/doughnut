package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_DIFFERENT_PART_AS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.AnswerViewedByUser;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
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
class FromDifferentPartAsQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note perspective;
  Note subjective;
  Note objective;
  Note ugly;
  Note pretty;
  Note tall;
  Note kind;
  Link subjectivePerspective;
  Link kindSubjective;
  ReviewPoint uglySubjectiveRp;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote("top").creatorAndOwner(userModel).please();
    perspective = makeMe.aNote("perspective").under(top).please();
    subjective = makeMe.aNote("subjective").under(top).please();
    objective = makeMe.aNote("objective").under(top).please();
    ugly = makeMe.aNote("ugly").under(top).please();
    pretty = makeMe.aNote("pretty").under(top).please();
    kind = makeMe.aNote("kind").under(top).please();
    tall = makeMe.aNote("tall").under(top).please();
    subjectivePerspective =
        makeMe.aLink().between(subjective, perspective, Link.LinkType.PART).please();
    makeMe.aLink().between(objective, perspective, Link.LinkType.PART).please();
    kindSubjective = makeMe.aLink().between(kind, subjective, Link.LinkType.TAGGED_BY).please();
    Link uglySubjective =
        makeMe.aLink().between(ugly, subjective, Link.LinkType.TAGGED_BY).please();
    uglySubjectiveRp = makeMe.aReviewPointFor(uglySubjective).by(userModel).inMemoryPlease();
    makeMe.refresh(top);
  }

  @Test
  void shouldBeInvalidWhenNoCousin() {
    assertThat(buildQuestion(), nullValue());
  }

  @Nested
  class WhenThereIsACousin {
    Link prettySubjective;

    @BeforeEach
    void setup() {
      prettySubjective =
          makeMe.aLink().between(pretty, subjective, Link.LinkType.TAGGED_BY).please();
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
        makeMe.aReviewPointFor(kindSubjective).by(userModel).please();
        makeMe.refresh(userModel.getEntity());
      }

      @Test
      void noRightAnswers() {
        makeMe.refresh(userModel.getEntity());
        assertThat(buildQuestion(), nullValue());
      }

      @Nested
      class WhenThereIsEnoughFillingOption {

        @BeforeEach
        void setup() {
          makeMe.aReviewPointFor(prettySubjective).by(userModel).please();
          makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswersAndFillingOptions() {
          QuizQuestionViewedByUser quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getDescription(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> a <em>DIFFERENT</em> part of <mark>perspective</mark> than:"));
          assertThat(quizQuestion.getMainTopic(), containsString(ugly.getTitle()));
          List<String> strings = toOptionStrings(quizQuestion);
          assertThat(tall.getTitle(), in(strings));
          assertThat(kind.getTitle(), in(strings));
          assertThat(pretty.getTitle(), in(strings));
          assertThat(ugly.getTitle(), not(in(strings)));
        }

        @Nested
        class WhenTheSupposedDifferentChoiceIsAlsoHavingTheSamePart {
          @BeforeEach
          void setup() {
            makeMe.aLink().between(tall, subjective, Link.LinkType.TAGGED_BY).please();
          }

          @Test
          void noRightAnswers() {
            makeMe.refresh(userModel.getEntity());
            assertThat(buildQuestion(), nullValue());
          }
        }

        @Nested
        class WhenTheReviewingSourceNoteIsAlsoTaggedByADifferentPart {

          @BeforeEach
          void setup() {
            makeMe.aLink().between(ugly, objective, Link.LinkType.TAGGED_BY).please();
          }

          @Test
          void noRightAnswers() {
            makeMe.refresh(userModel.getEntity());
            assertThat(buildQuestion(), nullValue());
          }

          @Nested
          class thereIsAThirdPerspective {
            Note pi;

            @BeforeEach
            void setup() {
              Note axiom = makeMe.aNote("objective").under(top).please();
              makeMe.aLink().between(axiom, perspective, LinkType.PART).please();
              pi = makeMe.aNote("pi").under(top).please();
              makeMe.aLink().between(pi, axiom, LinkType.TAGGED_BY).please();
            }

            @Test
            void thereIsAThirdPerspective() {
              makeMe.refresh(userModel.getEntity());
              assertThat(buildQuestion(), not(nullValue()));
            }

            @Test
            void whenTheOptionOfTheThirdPerspectiveIsAlsoObjective() {
              makeMe.aLink().between(pi, objective, LinkType.TAGGED_BY).please();
              makeMe.refresh(userModel.getEntity());
              assertThat(buildQuestion(), not(nullValue())); // wrong, this should be null
            }
          }
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
            assertThat(
                quizQuestion.getViceReviewPointIdList(), contains(additionalReviewPoint.getId()));
          }
        }

        @Nested
        class Answer {

          @Test
          void correct() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(uglySubjectiveRp)
                    .validQuestionOfType(FROM_DIFFERENT_PART_AS)
                    .answerWithSpelling(tall.getTitle())
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
          }

          @Test
          void wrongWhenChooseCousin() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(uglySubjectiveRp)
                    .validQuestionOfType(FROM_DIFFERENT_PART_AS)
                    .answerWithSpelling(pretty.getTitle())
                    .inMemoryPlease();
            assertFalse(answerResult.correct);
          }
        }
      }
    }
  }

  private QuizQuestionViewedByUser buildQuestion() {
    return makeMe.buildAQuestion(FROM_DIFFERENT_PART_AS, uglySubjectiveRp);
  }

  private List<String> toOptionStrings(QuizQuestionViewedByUser quizQuestion) {
    List<QuizQuestionViewedByUser.Option> options = quizQuestion.getOptions();
    return options.stream().map(QuizQuestionViewedByUser.Option::getDisplay).toList();
  }
}
