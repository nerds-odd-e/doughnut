package com.odde.doughnut.controllers;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
      assertThrows(ResponseStatusException.class, () -> nullUserController().repeatReview(null));
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
            makeMe.aNote("title").creatorAndOwner(anotherUser).description("description").please();
      }

      @Test
      void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(anotherUser).please();
        answer = makeMe.anAnswer().ofQuestion(SPELLING, reviewPoint).please();
        assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showAnswer(answer));
      }

      @Test
      void canSeeNoteThatHasReadAccess() throws UnexpectedNoAccessRightException {
        reviewPoint = makeMe.aReviewPointFor(noteByAnotherUser).by(currentUser).please();
        answer =
            makeMe.anAnswer().ofQuestion(SPELLING, reviewPoint).answerWithSpelling("xx").please();
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
