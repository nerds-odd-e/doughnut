package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.FROM_DIFFERENT_PART_AS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
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
    class WhenThereIsFillingChoice {

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
      class WhenThereIsEnoughFillingChoice {

        @BeforeEach
        void setup() {
          makeMe.aReviewPointFor(prettySubjective).by(userModel).please();
          makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswersAndFillingOptions() {
          QuizQuestion quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getStem(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> a <em>DIFFERENT</em> part of <mark>perspective</mark> than:"));
          assertThat(quizQuestion.getMainTopic(), containsString(ugly.getTopicConstructor()));
          List<String> strings = toOptionStrings(quizQuestion);
          assertThat(tall.getTopicConstructor(), in(strings));
          assertThat(kind.getTopicConstructor(), in(strings));
          assertThat(pretty.getTopicConstructor(), in(strings));
          assertThat(ugly.getTopicConstructor(), not(in(strings)));
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
        class Answer {

          @Test
          void correct() {
            AnsweredQuestion answerResult =
                makeMe
                    .anAnswerViewedByUser()
                    .validQuestionOfType(FROM_DIFFERENT_PART_AS, uglySubjectiveRp)
                    .choiceIndex(2)
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
          }

          @Test
          void wrongWhenChooseCousin() {
            AnsweredQuestion answerResult =
                makeMe
                    .anAnswerViewedByUser()
                    .validQuestionOfType(FROM_DIFFERENT_PART_AS, uglySubjectiveRp)
                    .choiceIndex(1)
                    .inMemoryPlease();
            assertFalse(answerResult.correct);
          }
        }
      }
    }
  }

  private QuizQuestion buildQuestion() {
    return makeMe.buildAQuestion(FROM_DIFFERENT_PART_AS, uglySubjectiveRp);
  }

  private List<String> toOptionStrings(QuizQuestion quizQuestion) {
    List<QuizQuestion.Choice> choices = quizQuestion.getChoices();
    return choices.stream().map(QuizQuestion.Choice::getDisplay).toList();
  }
}
