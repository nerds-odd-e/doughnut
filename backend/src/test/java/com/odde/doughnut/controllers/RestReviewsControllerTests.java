package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.json.DueReviewPoints;
import com.odde.doughnut.controllers.json.InitialInfo;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
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
      assertThrows(
          ResponseStatusException.class, () -> nullUserController().overview("Asia/Shanghai"));
    }
  }

  @Nested
  class initalReview {
    @Test
    void initialReview() {
      Note n = makeMe.aNote().creatorAndOwner(currentUser).please();
      makeMe.refresh(n);
      assertThat(n.getThing().getId(), notNullValue());
      List<ReviewPoint> reviewPointWithReviewSettings = controller.initialReview("Asia/Shanghai");
      assertThat(reviewPointWithReviewSettings, hasSize(1));
    }

    @Test
    void notLoggedIn() {
      assertThrows(
          ResponseStatusException.class, () -> nullUserController().initialReview("Asia/Shanghai"));
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
          ResponseStatusException.class,
          () -> nullUserController().repeatReview("Asia/Shanghai", null));
    }

    @ParameterizedTest
    @CsvSource(
        useHeadersInDisplayName = true,
        delimiter = '|',
        textBlock =
            """
       next review at (in hours) | timezone     | expected count
      #------------------------------------------------------------
       -1                        | Asia/Tokyo   | 1
       0                         | Asia/Tokyo   | 1
       4                         | Asia/Tokyo   | 0
       4                         | Europe/Paris | 1
       12                        | Europe/Paris | 0
       """)
    void shouldGetReviewPointsBasedOnTimezone(
        int nextReviewAtHours, String timezone, int expectedCount) {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe
          .aReviewPointBy(currentUser)
          .nextReviewAt(TimestampOperations.addHoursToTimestamp(currentTime, nextReviewAtHours))
          .please();
      DueReviewPoints dueReviewPoints = controller.repeatReview(timezone, null);
      assertThat(dueReviewPoints.getToRepeat(), hasSize(expectedCount));
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
        noteByAnotherUser =
            makeMe.aNote("title").creatorAndOwner(anotherUser).details("description").please();
      }

      @Test
      void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(anotherUser).please();
        answer = makeMe.anAnswer().ofSpellingQuestion(reviewPoint.getThing()).please();
        assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showAnswer(answer));
      }

      @Test
      void canSeeNoteThatHasReadAccess() throws UnexpectedNoAccessRightException {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(currentUser).please();
        answer =
            makeMe
                .anAnswer()
                .ofSpellingQuestion(reviewPoint.getThing())
                .answerWithSpelling("xx")
                .please();
        makeMe
            .aSubscription()
            .forUser(currentUser.getEntity())
            .forNotebook(noteByAnotherUser.getNotebook())
            .please();
        makeMe.refresh(currentUser.getEntity());
        AnsweredQuestion answeredQuestion = controller.showAnswer(answer);
        assertThat(answeredQuestion.answerId, equalTo(answer.getId()));
      }
    }
  }
}
