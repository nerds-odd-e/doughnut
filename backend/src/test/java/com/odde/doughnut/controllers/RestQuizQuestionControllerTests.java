package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
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
class RestQuizQuestionControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  RestQuizQuestionController controller;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestQuizQuestionController(modelFactoryService, currentUser, testabilitySettings);
  }

  RestQuizQuestionController nullUserController() {
    return new RestQuizQuestionController(
        modelFactoryService, makeMe.aNullUserModel(), testabilitySettings);
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    QuizQuestionEntity quizQuestionEntity;
    Answer answer;

    @BeforeEach
    void setup() {
      Note answerNote = makeMe.aNote().asHeadNoteOfANotebook().rememberSpelling().please();
      reviewPoint =
          makeMe
              .aReviewPointFor(answerNote)
              .by(currentUser)
              .forgettingCurveAndNextReviewAt(200)
              .please();
      quizQuestionEntity =
          makeMe.aQuestion().of(QuizQuestionEntity.QuestionType.SPELLING, reviewPoint).please();
      answer = makeMe.anAnswer().answerWithSpelling(answerNote.getTopic()).inMemoryPlease();
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnsweredQuestion answerResult = controller.answerQuiz(quizQuestionEntity, answer);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(reviewPoint.getLastReviewedAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestionEntity, answer);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestionEntity, answer);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          reviewPoint.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      Answer answer = new Answer();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(quizQuestionEntity, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        quizQuestionEntity =
            makeMe.aQuestion().of(QuizQuestionEntity.QuestionType.SPELLING, reviewPoint).please();
        answer = makeMe.anAnswer().answerWithSpelling("wrong").inMemoryPlease();
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
        AnsweredQuestion answerResult = controller.answerQuiz(quizQuestionEntity, answer);
        assertFalse(answerResult.correct);
        assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastReviewedAtTime() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Timestamp lastReviewedAt = reviewPoint.getLastReviewedAt();
        Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
        controller.answerQuiz(quizQuestionEntity, answer);
        assertThat(reviewPoint.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(reviewPoint.getLastReviewedAt(), equalTo(lastReviewedAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(quizQuestionEntity, answer);
        assertThat(
            reviewPoint.getNextReviewAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class MarkGoodQuestion {
    User user;
    QuizQuestionEntity quizQuestionEntity;
    Note note;
    ReviewPoint reviewPoint;

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
    }

    @Test
    void createMarkedGoodQuestion() {
      Integer markedQuestionId = controller.markQuestion(quizQuestionEntity);
      Optional<MarkedQuestion> markedQuestionRepositoryById =
          modelFactoryService.markedQuestionRepository.findById(markedQuestionId);
      assertFalse(markedQuestionRepositoryById.isEmpty());
      MarkedQuestion markedQuestion = markedQuestionRepositoryById.get();
      assertEquals(quizQuestionEntity.getId(), markedQuestion.getQuizQuestion().getId());
      assertEquals(note.getId(), markedQuestion.getNote().getId());
    }

    @Test
    void createMarkedQuestionInDatabase() {
      long oldCount = modelFactoryService.markedQuestionRepository.count();
      controller.markQuestion(quizQuestionEntity);
      assertThat(modelFactoryService.markedQuestionRepository.count(), equalTo(oldCount + 1));
    }
  }
}
