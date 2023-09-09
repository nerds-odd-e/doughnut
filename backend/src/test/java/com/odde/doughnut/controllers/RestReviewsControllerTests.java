package com.odde.doughnut.controllers;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.AnsweredQuestion;
import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.InitialInfo;
import com.odde.doughnut.entities.json.MarkedQuestionRequest;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import java.util.Optional;
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
            makeMe.aNote("title").creatorAndOwner(anotherUser).details("description").please();
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

  @Nested
  class MarkGoodQuestion {
    User user;
    QuizQuestionEntity quizQuestionEntity;
    Note note;
    ReviewPoint reviewPoint;
    MarkedQuestionRequest markedQuestionRequest;

    @BeforeEach
    void setup() throws QuizQuestionNotPossibleException {
      note = makeMe.aNote("new").creatorAndOwner(currentUser).please();

      user = currentUser.getEntity();
      reviewPoint =
          makeMe.aReviewPointFor(note).by(currentUser).forgettingCurveAndNextReviewAt(200).please();
      quizQuestionEntity =
          makeMe.aQuestion().of(QuizQuestionEntity.QuestionType.SPELLING, reviewPoint).please();
      modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
      modelFactoryService.noteRepository.save(note);
      markedQuestionRequest =
          new MarkedQuestionRequest() {
            {
              this.quizQuestionId = quizQuestionEntity.getId();
              this.noteId = note.getId();
            }
          };
    }

    @Test
    void createMarkedGoodQuestion() {
      Integer markedQuestionId = controller.markQuestion(markedQuestionRequest);
      Optional<MarkedQuestion> markedQuestionRepositoryById =
          modelFactoryService.markedQuestionRepository.findById(markedQuestionId);
      assertFalse(markedQuestionRepositoryById.isEmpty());
      MarkedQuestion markedQuestion = markedQuestionRepositoryById.get();
      assertEquals(markedQuestionRequest.quizQuestionId, markedQuestion.getQuizQuestionId());
      assertEquals(markedQuestionRequest.noteId, markedQuestion.getNoteId());
    }

    @Test
    void testCreateMarkedBadQuestionWithComment() {
      String badComment = "This is a bad question!";
      markedQuestionRequest.setComment(badComment);
      Integer markedQuestionId = controller.markQuestion(markedQuestionRequest);
      Optional<MarkedQuestion> markedQuestionRepositoryById =
          modelFactoryService.markedQuestionRepository.findById(markedQuestionId);
      assertFalse(markedQuestionRepositoryById.isEmpty());
      MarkedQuestion markedQuestion = markedQuestionRepositoryById.get();
      assertEquals(markedQuestionRequest.quizQuestionId, markedQuestion.getQuizQuestionId());
      assertEquals(markedQuestionRequest.noteId, markedQuestion.getNoteId());
      assertEquals(badComment, markedQuestion.getComment());
    }

    @Test
    void createMarkedQuestionInDatabase() {
      long oldCount = modelFactoryService.markedQuestionRepository.count();
      controller.markQuestion(markedQuestionRequest);
      assertThat(modelFactoryService.markedQuestionRepository.count(), equalTo(oldCount + 1));
    }

    @Test
    void deleteMarkedQuestion() {
      Integer markedQuestionId = controller.markQuestion(markedQuestionRequest);
      long oldCount = modelFactoryService.markedQuestionRepository.count();
      controller.deleteMarkQuestion(markedQuestionId);
      assertThat(modelFactoryService.markedQuestionRepository.count(), equalTo(oldCount - 1));
    }

    @Test
    void downloadMarkedQuestion() {
      controller.markQuestion(markedQuestionRequest);
      List<MarkedQuestion> questions = controller.getAllMarkedQuestions();
      assertThat(questions, hasSize(1));
      assertThat(questions.get(0).getUserId(), equalTo(user.getId()));
    }
  }
}
