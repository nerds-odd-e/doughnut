package com.odde.doughnut.controllers;

import static com.odde.doughnut.entities.SelfEvaluate.happy;
import static com.odde.doughnut.entities.SelfEvaluate.sad;
import static com.odde.doughnut.entities.SelfEvaluate.satisfying;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.SelfEvaluate;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.ReviewPointViewedByUser;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestReviewsControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
  }

  RestReviewsController controller() {
    return new RestReviewsController(
        modelFactoryService, new TestCurrentUserFetcher(userModel), testabilitySettings);
  }

  @Nested
  class overall {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModel();
      assertThrows(ResponseStatusException.class, () -> controller().overview());
    }
  }

  @Nested
  class initalReview {
    @Test
    void initialReview() {
      Note n = makeMe.aNote().creatorAndOwner(userModel).please();
      makeMe.refresh(n);
      assertThat(n.getThing().getId(), notNullValue());
      List<ReviewPointViewedByUser> reviewPointViewedByUsers = controller().initialReview();
      assertThat(reviewPointViewedByUsers, hasSize(1));
    }

    @Test
    void notLoggedIn() {
      userModel = makeMe.aNullUserModel();
      assertThrows(ResponseStatusException.class, () -> controller().initialReview());
    }
  }

  @Nested
  class createInitialReviewPoiint {
    @Test
    void create() {
      userModel = makeMe.aNullUserModel();
      InitialInfo info = new InitialInfo();
      assertThrows(ResponseStatusException.class, () -> controller().create(info));
    }
  }

  @Nested
  class repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModel();
      assertThrows(ResponseStatusException.class, () -> controller().repeatReview());
    }
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    Note note1;
    Answer answer = new Answer();

    @BeforeEach
    void setup() {
      note1 = makeMe.aNote().please();
      reviewPoint =
          makeMe
              .aReviewPointFor(note1)
              .by(userModel)
              .repetitionCount(5)
              .forgettiveCurveIndex(200)
              .please();
      QuizQuestion quizQuestion =
          makeMe
              .aQuestion()
              .of(QuizQuestion.QuestionType.CLOZE_SELECTION, reviewPoint)
              .inMemoryPlease();
      answer.setQuestion(quizQuestion);
      answer.setAnswerNoteId(note1.getId());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnswerResult answerResult = controller().answerQuiz(answer);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldValidateTheWrongAnswer() {
      QuizQuestion quizQuestion =
          makeMe.aQuestion().of(QuizQuestion.QuestionType.SPELLING, reviewPoint).inMemoryPlease();
      answer.setQuestion(quizQuestion);
      answer.setAnswerNoteId(null);
      answer.setSpellingAnswer("wrong");
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnswerResult answerResult = controller().answerQuiz(answer);
      assertFalse(answerResult.correct);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldIncreaseTheViceReviewPointToo() {
      Note note2 = makeMe.aNote().please();
      ReviewPoint anotherReviewPoint = makeMe.aReviewPointFor(note2).by(userModel).please();
      answer.getQuestion().setViceReviewPoints(List.of(anotherReviewPoint));
      makeMe.refresh(anotherReviewPoint);

      Integer oldForgettingCurveIndex = anotherReviewPoint.getForgettingCurveIndex();
      Integer oldRepetitionCount = anotherReviewPoint.getRepetitionCount();
      AnswerResult answerResult = controller().answerQuiz(answer);
      assertTrue(answerResult.correct);
      assertThat(
          anotherReviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(anotherReviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModel();
      Answer answer = new Answer();
      assertThrows(ResponseStatusException.class, () -> controller().answerQuiz(answer));
    }
  }

  @Nested
  class evaluate {

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModel();
      ReviewPoint reviewPoint = new ReviewPoint();
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.selfEvaluation = happy;
            }
          };
      assertThrows(
          ResponseStatusException.class,
          () -> controller().selfEvaluate(reviewPoint, selfEvaluation));
    }

    @Test
    void whenTheReviewPointDoesNotExist() {
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.selfEvaluation = happy;
            }
          };
      assertThrows(
          ResponseStatusException.class, () -> controller().selfEvaluate(null, selfEvaluation));
    }

    @Nested
    class WhenThereIsAReviewPoint {
      ReviewPoint rp;
      final int expectedSatisfyingForgettingCurveIndex = 110;

      @BeforeEach
      void setup() {
        rp = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
      }

      @Test
      void repeat() {
        evaluate(satisfying);
        assertThat(rp.getForgettingCurveIndex(), equalTo(expectedSatisfyingForgettingCurveIndex));
        assertThat(rp.getRepetitionCount(), equalTo(0));
      }

      private void evaluate(SelfEvaluate evaluation) {
        SelfEvaluation selfEvaluation =
            new SelfEvaluation() {
              {
                this.selfEvaluation = evaluation;
              }
            };
        controller().selfEvaluate(rp, selfEvaluation);
      }

      @Test
      void repeatSad() {
        evaluate(sad);
        assertThat(rp.getForgettingCurveIndex(), lessThan(expectedSatisfyingForgettingCurveIndex));
        assertThat(rp.getRepetitionCount(), equalTo(0));
      }

      @Test
      void repeatHappy() {
        evaluate(happy);
        assertThat(
            rp.getForgettingCurveIndex(), greaterThan(expectedSatisfyingForgettingCurveIndex));
        assertThat(rp.getRepetitionCount(), equalTo(0));
      }
    }
  }
}
