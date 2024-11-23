package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.SelfEvaluation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestReviewPointControllerTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;

  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  private UserModel userModel;
  RestReviewPointController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestReviewPointController(modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class Show {
    @Nested
    class WhenThereIsAReviewPoint {
      ReviewPoint rp;

      @BeforeEach
      void setup() {
        rp = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
      }

      @Test
      void shouldBeAbleToSeeOwn() throws UnexpectedNoAccessRightException {
        ReviewPoint reviewPoint = controller.show(rp);
        assertThat(reviewPoint, equalTo(rp));
      }

      @Test
      void shouldNotBeAbleToSeeOthers() {
        rp = makeMe.aReviewPointBy(makeMe.aUser().toModelPlease()).please();
        assertThrows(UnexpectedNoAccessRightException.class, () -> controller.show(rp));
      }

      @Test
      void removeAndUpdateLastReviewedAt() {
        controller.removeFromRepeating(rp);
        assertThat(rp.getRemovedFromReview(), is(true));
        assertThat(rp.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      }
    }
  }

  @Nested
  class Evaluate {

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModelPlease();
      ReviewPoint reviewPoint = makeMe.aReviewPointFor(makeMe.aNote().please()).inMemoryPlease();
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.adjustment = 1;
            }
          };
      assertThrows(
          ResponseStatusException.class,
          () -> controller.selfEvaluate(reviewPoint, selfEvaluation));
    }

    @Test
    void whenTheReviewPointDoesNotExist() {
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.adjustment = 1;
            }
          };
      assertThrows(
          ResponseStatusException.class, () -> controller.selfEvaluate(null, selfEvaluation));
    }

    @Nested
    class WhenThereIsAReviewPoint {
      ReviewPoint rp;

      @BeforeEach
      void setup() {
        rp = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
      }

      @Test
      void repeat() {
        evaluate(1);
        assertThat(rp.getForgettingCurveIndex(), equalTo(101));
        assertThat(rp.getRepetitionCount(), equalTo(0));
      }

      private void evaluate(int adj) {
        SelfEvaluation selfEvaluation =
            new SelfEvaluation() {
              {
                this.adjustment = adj;
              }
            };
        controller.selfEvaluate(rp, selfEvaluation);
      }
    }
  }

  @Nested
  class MarkAsReviewed {
    @Test
    void itMustUpdateTheReviewPointRecord() {
      Note note = makeMe.aNote().please();
      ReviewPoint rp = makeMe.aReviewPointFor(note).by(userModel).please();
      Integer oldRepetitionCount = rp.getRepetitionCount();
      controller.markAsRepeated(rp, true);
      assertThat(rp.getRepetitionCount(), equalTo(oldRepetitionCount + 1));
    }
  }

  @Nested
  class GetRecentReviewPoints {
    @Test
    void shouldReturnEmptyListWhenNoReviewPoints() {
      List<ReviewPoint> reviewPoints = controller.getRecentReviewPoints();
      assertThat(reviewPoints, empty());
    }

    @Test
    void shouldReturnReviewPointsForCurrentUser() {
      ReviewPoint rp1 = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
      ReviewPoint rp2 = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();

      List<ReviewPoint> reviewPoints = controller.getRecentReviewPoints();

      assertThat(reviewPoints, hasSize(2));
      assertThat(reviewPoints, containsInAnyOrder(rp1, rp2));
    }

    @Test
    void shouldNotReturnReviewPointsFromOtherUsers() {
      UserModel otherUser = makeMe.aUser().toModelPlease();
      makeMe.aReviewPointBy(otherUser).please();

      List<ReviewPoint> reviewPoints = controller.getRecentReviewPoints();

      assertThat(reviewPoints, empty());
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller =
          new RestReviewPointController(modelFactoryService, userModel, testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentReviewPoints());
    }
  }

  @Nested
  class GetRecentlyReviewedPoints {
    @Test
    void shouldReturnEmptyListWhenNoReviewedPoints() {
      List<ReviewPoint> reviewPoints = controller.getRecentlyReviewedPoints();
      assertThat(reviewPoints, empty());
    }

    @Test
    void shouldReturnRecentlyReviewedPointsForCurrentUser() {
      ReviewPoint rp1 = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();
      ReviewPoint rp2 = makeMe.aReviewPointFor(makeMe.aNote().please()).by(userModel).please();

      // Mark as reviewed
      controller.markAsRepeated(rp1, true);
      controller.markAsRepeated(rp2, true);

      List<ReviewPoint> reviewPoints = controller.getRecentlyReviewedPoints();

      assertThat(reviewPoints, hasSize(2));
      assertThat(reviewPoints, containsInAnyOrder(rp1, rp2));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      userModel = makeMe.aNullUserModelPlease();
      controller =
          new RestReviewPointController(modelFactoryService, userModel, testabilitySettings);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentlyReviewedPoints());
    }
  }
}
