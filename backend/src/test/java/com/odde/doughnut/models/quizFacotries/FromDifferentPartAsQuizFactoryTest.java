package com.odde.doughnut.models.quizFacotries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.FromDifferentPartAsPredefinedFactory;
import com.odde.doughnut.models.randomizers.NonRandomizer;
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
  User user;
  Note top;
  Note perspective;
  Note subjective;
  Note objective;
  Note ugly;
  Note pretty;
  Note tall;
  Note kind;
  Note subjectivePerspective;
  Note kindSubjective;
  Note uglySubjective;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    top = makeMe.aNote("top").creatorAndOwner(user).please();
    perspective = makeMe.aNote("perspective").under(top).please();
    subjective = makeMe.aNote("subjective").under(top).please();
    objective = makeMe.aNote("objective").under(top).please();
    ugly = makeMe.aNote("ugly").under(top).please();
    pretty = makeMe.aNote("pretty").under(top).please();
    kind = makeMe.aNote("kind").under(top).please();
    tall = makeMe.aNote("tall").under(top).please();
    subjectivePerspective = makeMe.aLink().between(subjective, perspective, LinkType.PART).please();
    makeMe.aLink().between(objective, perspective, LinkType.PART).please();
    kindSubjective = makeMe.aLink().between(kind, subjective, LinkType.TAGGED_BY).please();
    uglySubjective = makeMe.aLink().between(ugly, subjective, LinkType.TAGGED_BY).please();
  }

  @Test
  void shouldBeInvalidWhenNoCousin() {
    assertThat(buildQuestion(), nullValue());
  }

  @Nested
  class WhenThereIsACousin {
    Note prettySubjective;

    @BeforeEach
    void setup() {
      prettySubjective = makeMe.aLink().between(pretty, subjective, LinkType.TAGGED_BY).please();
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
        makeMe.aMemoryTrackerFor(kindSubjective).by(user).please();
      }

      @Test
      void noRightAnswers() {
        assertThat(buildQuestion(), nullValue());
      }

      @Nested
      class WhenThereIsEnoughFillingChoice {

        @BeforeEach
        void setup() {
          makeMe.aMemoryTrackerFor(prettySubjective).by(user).please();
        }

        @Test
        void shouldIncludeRightAnswersAndFillingOptions() {
          PredefinedQuestion predefinedQuestion = buildQuestion();
          assertThat(
              predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
              containsString(
                  "<p>Which one <mark>is tagged by</mark> a <em>DIFFERENT</em> part of <mark>perspective</mark> than:"));
          assertThat(
              predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getStem(),
              containsString(ugly.getTopicConstructor()));
          List<String> strings =
              predefinedQuestion.getBareQuestion().getMultipleChoicesQuestion().getChoices();
          assertThat(tall.getTopicConstructor(), in(strings));
          assertThat(kind.getTopicConstructor(), in(strings));
          assertThat(pretty.getTopicConstructor(), in(strings));
          assertThat(ugly.getTopicConstructor(), not(in(strings)));
        }

        @Nested
        class WhenTheSupposedDifferentChoiceIsAlsoHavingTheSamePart {
          @BeforeEach
          void setup() {
            makeMe.aLink().between(tall, subjective, LinkType.TAGGED_BY).please();
          }

          @Test
          void noRightAnswers() {
            assertThat(buildQuestion(), nullValue());
          }
        }

        @Nested
        class WhenTheRecallServiceSourceNoteIsAlsoTaggedByADifferentPart {

          @BeforeEach
          void setup() {
            makeMe.aLink().between(ugly, objective, LinkType.TAGGED_BY).please();
          }

          @Test
          void noRightAnswers() {
            PredefinedQuestion actual = buildQuestion();
            assertThat(actual, nullValue());
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
              assertThat(buildQuestion(), not(nullValue()));
            }

            @Test
            void whenTheOptionOfTheThirdPerspectiveIsAlsoObjective() {
              makeMe.aLink().between(pi, objective, LinkType.TAGGED_BY).please();
              assertThat(buildQuestion(), not(nullValue())); // wrong, this should be null
            }
          }
        }

        @Nested
        class Answer {
          PredefinedQuestionFactory predefinedQuestionFactory;

          @BeforeEach
          void setup() {
            predefinedQuestionFactory = getQuizQuestionFactory();
          }

          @Test
          void correct() {
            AnsweredQuestion answerResult =
                makeMe
                    .aReviewQuestionInstance()
                    .useFactory(predefinedQuestionFactory)
                    .answerChoiceIndex(2)
                    .please(false)
                    .getAnsweredQuestion();
            assertTrue(answerResult.answer.getCorrect());
          }

          @Test
          void wrongWhenChooseCousin() {
            AnsweredQuestion answerResult =
                makeMe
                    .aReviewQuestionInstance()
                    .useFactory(predefinedQuestionFactory)
                    .answerChoiceIndex(1)
                    .please(false)
                    .getAnsweredQuestion();
            assertFalse(answerResult.answer.getCorrect());
          }
        }
      }
    }
  }

  private PredefinedQuestion buildQuestion() {
    try {
      return getQuizQuestionFactory().buildValidPredefinedQuestion();
    } catch (PredefinedQuestionNotPossibleException e) {
      return null;
    }
  }

  private PredefinedQuestionFactory getQuizQuestionFactory() {
    PredefinedQuestionServant servant =
        new PredefinedQuestionServant(user, new NonRandomizer(), makeMe.modelFactoryService);
    return new FromDifferentPartAsPredefinedFactory(uglySubjective, servant);
  }
}
