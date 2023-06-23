package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
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
class RestReviewPointControllerTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;

  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  private UserModel userModel;
  RestReviewPointController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestReviewPointController(null, modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class WhenThereIsAReviewPoint {
    ReviewPoint rp;

    @BeforeEach
    void setup() {
      rp = makeMe.aReviewPointFor(makeMe.aHeadNote().please()).by(userModel).please();
    }

    @Test
    void shouldBeAbleToSeeOwn() throws UnexpectedNoAccessRightException {
      ReviewPoint reviewPoint = controller.show(rp);
      assertThat(reviewPoint, equalTo(rp));
    }

    @Test
    void shouldNotBeAbleToSeeOthers() {
      rp = makeMe.aReviewPointFor(makeMe.aHeadNote().please()).by(makeMe.aUser().please()).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.show(rp));
    }

    @Test
    void remove() {
      controller.removeFromRepeating(rp);
      assertThat(rp.getRemovedFromReview(), is(true));
    }
  }

  @Nested
  class evaluate {

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      userModel = makeMe.aNullUserModel();
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
}
