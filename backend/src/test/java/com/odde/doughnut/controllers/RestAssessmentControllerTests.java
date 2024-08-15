package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerSubmission;
import com.odde.doughnut.controllers.dto.AssessmentHistory;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestAssessmentControllerTests {
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private RestAssessmentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAssessmentController(
            openAiApi, makeMe.modelFactoryService, testabilitySettings, currentUser);
  }

  @Nested
  class assessmentQuestionOrderTest {
    private Notebook notebook;
    private Note topNote;
    private int representativeNumberOfAttempts = 30;

    Set<Integer> performAssessments(int numberOfAttempts) throws UnexpectedNoAccessRightException {
      Set<Integer> questionIds = new HashSet<>();
      for (int i = 0; i < numberOfAttempts; i++) {
        List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
        Integer questionId = assessment.get(0).getId();
        questionIds.add(questionId);
      }
      return questionIds;
    }

    @BeforeEach
    void setup() {
      testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.seed, 1));
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);
    }

    @Test
    void shouldPickRandomNotesForAssessment() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(10, NoteBuilder::hasAnApprovedQuestion).please();

      Set<Integer> questionIds = performAssessments(representativeNumberOfAttempts);

      assertTrue(questionIds.size() > 1, "Expected questions from different notes.");
    }

    @Test
    void shouldPickRandomQuestionsFromTheSameNote() throws UnexpectedNoAccessRightException {
      makeMe
          .theNote(topNote)
          .withNChildrenThat(1, noteBuilder -> noteBuilder.hasApprovedQuestions(10))
          .please();
      Set<Integer> questionIds = performAssessments(representativeNumberOfAttempts);
      assertTrue(questionIds.size() > 1, "Expected questions from the same note.");
    }
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
              openAiApi,
              makeMe.modelFactoryService,
              testabilitySettings,
              makeMe.aNullUserModelPlease());
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

    @Test
    void shouldReturn5QuestionsWhenThereAreMoreThan5NotesWithQuestions()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(5);
      List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldThrowExceptionWhenThereAreNotEnoughQuestions() {
      makeMe.theNote(topNote).withNChildrenThat(4, NoteBuilder::hasAnApprovedQuestion).please();
      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldGetOneQuestionFromEachNoteOnly() {
      makeMe
          .theNote(topNote)
          .withNChildrenThat(
              3,
              noteBuilder -> {
                noteBuilder.hasAnApprovedQuestion();
                noteBuilder.hasAnApprovedQuestion();
                noteBuilder.hasAnApprovedQuestion();
              })
          .please();

      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldThrowExceptionWhenThereExcessAssessmentLimit() {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(5);

      var now = testabilitySettings.getCurrentUTCTimestamp();
      for (int i = 0; i < 3; i++) {
        makeMe.aAssessmentAttempt(currentUser.getEntity(), notebook, now).please();
      }

      assertThrows(
          ResponseStatusException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldReturnQuestionsWhenAssessmentDoesNotExcessAssessmentLimitWithinOneDay()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(5);

      var now = testabilitySettings.getCurrentUTCTimestamp();
      for (int i = 0; i < 2; i++) {
        makeMe.aAssessmentAttempt(currentUser.getEntity(), notebook, now).please();
      }

      var yesterday = TimestampOperations.addHoursToTimestamp(now, -25);
      makeMe.aAssessmentAttempt(currentUser.getEntity(), notebook, yesterday).please();

      List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);
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
      List<AssessmentHistory> assessmentHistories = controller.getAssessmentHistory();
      assertEquals(0, assessmentHistories.size());
    }

    @Test
    void shouldReturnOneAssessmentHistory() {
      makeMe
          .aAssessmentAttempt(
              currentUser.getEntity(), notebook, testabilitySettings.getCurrentUTCTimestamp(), 2, 2)
          .please();
      List<AssessmentHistory> assessmentHistories = controller.getAssessmentHistory();
      assertEquals(1, assessmentHistories.size());
    }

    @Test
    void shouldReturnOnePassAssessmentHistory() {
      makeMe
          .aAssessmentAttempt(
              currentUser.getEntity(), notebook, testabilitySettings.getCurrentUTCTimestamp(), 5, 4)
          .please();
      List<AssessmentHistory> assessmentHistories = controller.getAssessmentHistory();
      assertEquals("Pass", assessmentHistories.getFirst().getResult());
    }

    @Test
    void shouldReturnOneFailAssessmentHistory() {
      makeMe
          .aAssessmentAttempt(
              currentUser.getEntity(), notebook, testabilitySettings.getCurrentUTCTimestamp(), 5, 2)
          .please();
      List<AssessmentHistory> assessmentHistories = controller.getAssessmentHistory();
      assertEquals("Fail", assessmentHistories.getFirst().getResult());
    }

    @Test
    void shouldReturnNoAssessmentHistoryForOtherUser() {
      User anotherUser = makeMe.aUser().please();
      makeMe
          .aAssessmentAttempt(
              anotherUser, notebook, testabilitySettings.getCurrentUTCTimestamp(), 5, 5)
          .please();
      List<AssessmentHistory> assessmentHistories = controller.getAssessmentHistory();
      assertEquals(0, assessmentHistories.size());
    }
  }

  @Nested
  class showCertificateTests {
    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void shouldNotReturnCertificate() throws UnexpectedNoAccessRightException {
      Certificate certificate = controller.getCertificate(notebook);
      assertNull(certificate);
    }

    @Test
    void shouldReturnCertificate() throws UnexpectedNoAccessRightException {
      AssessmentAttempt assessmentAttempt =
          makeMe
              .aAssessmentAttempt(
                  currentUser.getEntity(), notebook, testabilitySettings.getCurrentUTCTimestamp())
              .please();

      Certificate expectedCertificate = makeMe.aCertificate(assessmentAttempt).please();
      Certificate certificate = controller.getCertificate(notebook);
      assertEquals(expectedCertificate, certificate);
    }
  }
}
