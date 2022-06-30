package com.odde.doughnut.controllers;

import static com.odde.doughnut.entities.SelfEvaluate.*;
import static com.odde.doughnut.entities.SelfEvaluate.happy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.SelfEvaluate;
import com.odde.doughnut.entities.json.SelfEvaluation;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
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
  private UserModel userModel;
  RestReviewPointController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestReviewPointController(modelFactoryService, new TestCurrentUserFetcher(userModel));
  }

  @Nested
  class WhenThereIsAReviewPoint {
    ReviewPoint rp;

    @BeforeEach
    void setup() {
      rp = makeMe.aReviewPointFor(makeMe.aHeadNote().please()).by(userModel).please();
    }

    @Test
    void show() throws NoAccessRightException {
      ReviewPoint result = controller.show(rp);
      assertThat(result, notNullValue());
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
      ReviewPoint reviewPoint = new ReviewPoint();
      SelfEvaluation selfEvaluation =
          new SelfEvaluation() {
            {
              this.selfEvaluation = happy;
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
              this.selfEvaluation = happy;
            }
          };
      assertThrows(
          ResponseStatusException.class, () -> controller.selfEvaluate(null, selfEvaluation));
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
        controller.selfEvaluate(rp, selfEvaluation);
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
