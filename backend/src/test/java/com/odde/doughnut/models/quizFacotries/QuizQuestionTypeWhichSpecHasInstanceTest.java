package com.odde.doughnut.models.quizFacotries;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.WHICH_SPEC_HAS_INSTANCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
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
class WhichSpecHasInstanceQuizFactoryTest {
  @Autowired MakeMe makeMe;
  UserModel userModel;
  Note top;
  Note target;
  Note source;
  Note anotherSource;
  ReviewPoint reviewPoint;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    top = makeMe.aNote("top").creatorAndOwner(userModel).please();
    target = makeMe.aNote("element").under(top).please();
    source = makeMe.aNote("noble gas").under(top).linkTo(target, Link.LinkType.SPECIALIZE).please();
    anotherSource = makeMe.aNote("non-official name").under(top).please();
    reviewPoint = makeMe.aReviewPointFor(source.getLinks().get(0)).by(userModel).inMemoryPlease();
    makeMe.refresh(top);
  }

  @Test
  void shouldBeInvalidWhenNoInsatnceOfLink() {
    assertThat(buildQuestion(), nullValue());
  }

  @Nested
  class WhenTheNoteHasInstance {
    @BeforeEach
    void setup() {
      makeMe.theNote(source).linkTo(anotherSource, Link.LinkType.INSTANCE);
    }

    @Test
    void shouldBeInvalidWhenNoInsatnceOfLink() {
      assertThat(buildQuestion(), nullValue());
    }

    @Nested
    class WhenTheNoteHasMoreSpecificationSiblings {
      Note metal;

      @BeforeEach
      void setup() {
        metal = makeMe.aNote("metal").under(top).linkTo(target, Link.LinkType.SPECIALIZE).please();
      }

      @Test
      void shouldBeInvalidWhenNoViceReviewPoint() {
        assertThat(buildQuestion(), nullValue());
      }

      @Nested
      class WhenTheSecondLinkHasReviewPoint {

        @BeforeEach
        void setup() {
          makeMe.aReviewPointFor(source.getLinks().get(1)).by(userModel).please();
          makeMe.refresh(userModel.getEntity());
        }

        @Test
        void shouldIncludeRightAnswers() {
          QuizQuestionViewedByUser quizQuestion = buildQuestion();
          assertThat(
              quizQuestion.getDescription(),
              containsString(
                  "<p>Which one is a specialization of <mark>element</mark> <em>and</em> is an instance of <mark>non-official name</mark>:"));
          List<String> strings = toOptionStrings(quizQuestion);
          assertThat("metal", in(strings));
          assertThat(source.getTitle(), in(strings));
        }

        @Nested
        class Answer {
          @Test
          void correct() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(reviewPoint)
                    .validQuestionOfType(WHICH_SPEC_HAS_INSTANCE)
                    .answerWithSpelling(source.getTitle())
                    .inMemoryPlease();
            assertTrue(answerResult.correct);
          }

          @Test
          void wrong() {
            AnswerViewedByUser answerResult =
                makeMe
                    .anAnswerViewedByUserFor(reviewPoint)
                    .validQuestionOfType(WHICH_SPEC_HAS_INSTANCE)
                    .answerWithSpelling("metal")
                    .inMemoryPlease();
            assertFalse(answerResult.correct);
          }
        }

        @Nested
        class PersonAlsoHasTheSameNoteAsInstance {

          @BeforeEach
          void setup() {
            makeMe.theNote(metal).linkTo(anotherSource, Link.LinkType.INSTANCE).please();
          }

          @Test
          void shouldBeInvalid() {
            assertThat(buildQuestion(), nullValue());
          }
        }

        @Nested
        class OptionFromInstance {

          @BeforeEach
          void setup() {
            makeMe
                .aNote("something else")
                .under(top)
                .linkTo(anotherSource, Link.LinkType.INSTANCE)
                .please();
            makeMe.refresh(top);
          }

          @Test
          void options() {
            List<String> strings = toOptionStrings(buildQuestion());
            assertThat("something else", in(strings));
          }
        }
      }
    }
  }

  private QuizQuestionViewedByUser buildQuestion() {
    return makeMe.buildAQuestion(WHICH_SPEC_HAS_INSTANCE, this.reviewPoint);
  }

  private List<String> toOptionStrings(QuizQuestionViewedByUser quizQuestion) {
    List<QuizQuestionViewedByUser.Option> options = quizQuestion.getOptions();
    return options.stream().map(QuizQuestionViewedByUser.Option::getDisplay).toList();
  }
}
