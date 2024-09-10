package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerSubmission;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.NoteBuilder;
import java.sql.Timestamp;
import java.util.ArrayList;
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
public class RestAssessmentControllerTests {
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private RestAssessmentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAssessmentController(makeMe.modelFactoryService, testabilitySettings, currentUser);
  }

  @Nested
  class generateOnlineAssessmentTest {
    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void whenNotLogin() {
      controller =
          new RestAssessmentController(
              makeMe.modelFactoryService, testabilitySettings, makeMe.aNullUserModelPlease());
      assertThrows(
          ResponseStatusException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldNotBeAbleToAccessNotebookWhenUserHasNoPermission() {
      User anotherUser = makeMe.aUser().please();
      notebook.setOwnership(anotherUser.getOwnership());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldBeAbleToAccessNotebookThatIsInTheBazaar() throws UnexpectedNoAccessRightException {
      Note noteOwnedByOtherUser = makeMe.aNote().please();
      makeMe
          .theNote(noteOwnedByOtherUser)
          .withNChildrenThat(6, NoteBuilder::hasAnApprovedQuestion)
          .please();
      noteOwnedByOtherUser.getNotebook().getNotebookSettings().setNumberOfQuestionsInAssessment(5);
      BazaarNotebook bazaarNotebook =
          makeMe.aBazaarNotebook(noteOwnedByOtherUser.getNotebook()).please();
      List<QuizQuestion> assessment =
          controller.generateAssessmentQuestions(bazaarNotebook.getNotebook());
      assertEquals(5, assessment.size());
    }
  }

  @Nested
  class completeAssessmentTest {
    private Notebook notebook;
    private Note topNote;
    private List<AnswerSubmission> answerSubmissions;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      answerSubmissions = new ArrayList<>();
    }

    @Test
    void shouldIncludeTheNotebookCertificateInTheResult() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(3, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
      makeMe
          .modelFactoryService
          .notebookService(notebook)
          .requestNotebookApproval()
          .approve(makeMe.aTimestamp().please());
      makeMe.refresh(notebook);

      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, answerSubmissions);

      assertTrue(assessmentResult.isCertified());
    }

    @Test
    void shouldReturnAllAnswersCorrect() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(3, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(3);

      for (Note note : notebook.getNotes()) {
        QuizQuestionAndAnswer quizQuestionAndAnswer = note.getQuizQuestionAndAnswers().get(0);
        quizQuestionAndAnswer.setCorrectAnswerIndex(1);

        AnswerSubmission answerSubmission = new AnswerSubmission();
        answerSubmission.setQuestionId(quizQuestionAndAnswer.getId());

        answerSubmission.setAnswerId(0);
        answerSubmission.setCorrectAnswers(true);
        answerSubmissions.add(answerSubmission);
      }

      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, answerSubmissions);

      assertEquals(answerSubmissions.size(), assessmentResult.getTotalCount());
      assertEquals(3, assessmentResult.getCorrectCount());
    }

    @Test
    void shouldCreateNewAssessmentAttempt() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(3, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(3);

      for (Note note : notebook.getNotes()) {
        QuizQuestionAndAnswer quizQuestionAndAnswer = note.getQuizQuestionAndAnswers().get(0);
        quizQuestionAndAnswer.setCorrectAnswerIndex(1);

        AnswerSubmission answerSubmission = new AnswerSubmission();
        answerSubmission.setQuestionId(quizQuestionAndAnswer.getId());

        answerSubmission.setAnswerId(0);
        answerSubmission.setCorrectAnswers(true);
        answerSubmissions.add(answerSubmission);
      }

      Timestamp timestamp = makeMe.aTimestamp().please();
      testabilitySettings.timeTravelTo(timestamp);
      controller.submitAssessmentResult(notebook, answerSubmissions);
      AssessmentAttempt assessmentAttempt =
          makeMe.modelFactoryService.assessmentAttemptRepository.findAll().iterator().next();

      assertEquals(3, assessmentAttempt.getAnswersCorrect());
      assertEquals(timestamp, assessmentAttempt.getSubmittedAt());
    }

    @Test
    void shouldReturnSomeAnswersCorrect() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(3);

      QuizQuestionAndAnswer quizQuestionAndAnswer =
          notebook.getNotes().get(0).getQuizQuestionAndAnswers().get(0);
      quizQuestionAndAnswer.setCorrectAnswerIndex(0);

      AnswerSubmission answerSubmission = new AnswerSubmission();
      answerSubmission.setQuestionId(quizQuestionAndAnswer.getId());
      answerSubmission.setAnswerId(0);
      answerSubmission.setCorrectAnswers(true);
      answerSubmissions.add(answerSubmission);

      quizQuestionAndAnswer = notebook.getNotes().get(1).getQuizQuestionAndAnswers().get(0);
      quizQuestionAndAnswer.setCorrectAnswerIndex(0);

      answerSubmission = new AnswerSubmission();
      answerSubmission.setQuestionId(quizQuestionAndAnswer.getId());
      answerSubmission.setAnswerId(0);
      answerSubmission.setCorrectAnswers(false);
      answerSubmissions.add(answerSubmission);

      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, answerSubmissions);

      assertEquals(2, assessmentResult.getTotalCount());
      assertEquals(1, assessmentResult.getCorrectCount());
    }

    @Test
    void shouldNotBeAbleToAccessNotebookWhenUserHasNoPermission() {
      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
      User anotherUser = makeMe.aUser().please();
      notebook.setOwnership(anotherUser.getOwnership());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.submitAssessmentResult(notebook, answerSubmissions));
    }
  }

  @Nested
  class showAssessmentHistoryTest {
    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void shouldReturnEmptyIfNoAssemsentTaken() {
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertEquals(0, assessmentHistories.size());
    }

    @Test
    void shouldReturnOneAssessmentHistory() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertEquals(1, assessmentHistories.size());
    }

    @Test
    void shouldReturnOnePassAssessmentHistory() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(5, 4).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertTrue(assessmentHistories.getFirst().getIsPass());
    }

    @Test
    void shouldReturnOneFailAssessmentHistory() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(5, 2).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertFalse(assessmentHistories.getFirst().getIsPass());
    }

    @Test
    void shouldReturnNoAssessmentHistoryForOtherUser() {
      User anotherUser = makeMe.aUser().please();
      makeMe.anAssessmentAttempt(anotherUser, notebook).score(5, 5).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertEquals(0, assessmentHistories.size());
    }
  }
}
