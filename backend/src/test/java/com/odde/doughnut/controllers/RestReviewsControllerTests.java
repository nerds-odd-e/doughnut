package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
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
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  RestReviewsController controller;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestReviewsController(modelFactoryService, currentUser, testabilitySettings);
  }

  RestReviewsController nullUserController() {
    return new RestReviewsController(
        modelFactoryService, makeMe.aNullUserModel(), testabilitySettings);
  }

  @Nested
  class overall {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(ResponseStatusException.class, () -> nullUserController().overview());
    }
  }

  @Nested
  class initalReview {
    @Test
    void initialReview() {
      Note n = makeMe.aNote().creatorAndOwner(currentUser).please();
      makeMe.refresh(n);
      assertThat(n.getThing().getId(), notNullValue());
      List<ReviewPoint> reviewPointWithReviewSettings = controller.initialReview();
      assertThat(reviewPointWithReviewSettings, hasSize(1));
    }

    @Test
    void notLoggedIn() {
      assertThrows(ResponseStatusException.class, () -> nullUserController().initialReview());
    }
  }

  @Nested
  class createInitialReviewPoiint {
    @Test
    void create() {
      InitialInfo info = new InitialInfo();
      assertThrows(ResponseStatusException.class, () -> nullUserController().create(info));
    }
  }

  @Nested
  class repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(
          ResponseStatusException.class, () -> nullUserController().repeatReview( null));
    }
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    Answer answer;

    @BeforeEach
    void setup() {
      Note answerNote = makeMe.aNote().please();
      reviewPoint =
          makeMe
              .aReviewPointFor(answerNote)
              .by(currentUser)
              .forgettingCurveAndNextReviewAt(200)
              .please();
      answer =
          makeMe
              .anAnswerFor(reviewPoint)
              .type(QuizQuestion.QuestionType.CLOZE_SELECTION)
              .answerWithId(answerNote)
              .inMemoryPlease();
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnswerResult answerResult = controller.answerQuiz(answer);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(reviewPoint.getLastReviewedAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(answer);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(answer);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          reviewPoint.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldIncreaseTheViceReviewPointToo() {
      Note note2 = makeMe.aNote().please();
      ReviewPoint anotherReviewPoint = makeMe.aReviewPointFor(note2).by(currentUser).please();
      answer.getQuestion().setViceReviewPoints(List.of(anotherReviewPoint));
      makeMe.refresh(anotherReviewPoint);
      makeMe.refresh(note2);

      Integer oldForgettingCurveIndex = anotherReviewPoint.getForgettingCurveIndex();
      Integer oldRepetitionCount = anotherReviewPoint.getRepetitionCount();
      AnswerResult answerResult = controller.answerQuiz(answer);
      assertTrue(answerResult.correct);
      assertThat(
          anotherReviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(anotherReviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      Answer answer = new Answer();
      assertThrows(ResponseStatusException.class, () -> nullUserController().answerQuiz(answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answer =
            makeMe
                .anAnswerFor(reviewPoint)
                .type(QuizQuestion.QuestionType.SPELLING)
                .answerWithSpelling("wrong")
                .inMemoryPlease();
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
        AnswerResult answerResult = controller.answerQuiz(answer);
        assertFalse(answerResult.correct);
        assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastReviewedAtTime() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Timestamp lastReviewedAt = reviewPoint.getLastReviewedAt();
        Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
        controller.answerQuiz(answer);
        assertThat(reviewPoint.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(reviewPoint.getLastReviewedAt(), equalTo(lastReviewedAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(answer);
        assertThat(
            reviewPoint.getNextReviewAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class showAnswer {
    Answer answer;
    Note noteByAnotherUser;
    ReviewPoint reviewPoint;
    User anotherUser;

    @Nested
    class ANoteFromOtherUser {
      @BeforeEach
      void setup() {
        anotherUser = makeMe.aUser().please();
        noteByAnotherUser = makeMe.aNote().creatorAndOwner(anotherUser).please();
      }

      @Test
      void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(anotherUser).please();
        answer = makeMe.anAnswerFor(reviewPoint).please();
        assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showAnswer(answer));
      }

      @Test
      void canSeeNoteThatHasReadAccess() throws UnexpectedNoAccessRightException {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(currentUser).please();
        answer = makeMe.anAnswerFor(reviewPoint).answerWithSpelling("xx").please();
        makeMe
            .aSubscription()
            .forUser(currentUser.getEntity())
            .forNotebook(noteByAnotherUser.getNotebook())
            .please();
        AnswerViewedByUser answerViewedByUser = controller.showAnswer(answer);
        assertThat(answerViewedByUser.answerId, equalTo(answer.getId()));
      }
    }
  }
}
