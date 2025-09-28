package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.QuestionAnswerException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.NoteBuilder;
import java.sql.Timestamp;
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

  private AssessmentService assessmentService;

  @BeforeEach
  void setup() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestAssessmentController(makeMe.modelFactoryService, testabilitySettings, currentUser);
    assessmentService = new AssessmentService(makeMe.modelFactoryService, testabilitySettings);
  }

  @Nested
  class generateOnlineAssessmentTest {
    private Notebook notebook;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser).please();
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
      List<AssessmentQuestionInstance> assessment =
          controller
              .generateAssessmentQuestions(bazaarNotebook.getNotebook())
              .getAssessmentQuestionInstances();
      assertEquals(5, assessment.size());
    }
  }

  @Nested
  class answerQuestion {
    AssessmentQuestionInstance assessmentQuestionInstance;
    AnswerDTO answerDTO = new AnswerDTO();

    @BeforeEach
    void setup() {
      AssessmentAttempt assessmentAttempt =
          makeMe.anAssessmentAttempt(currentUser.getEntity()).withOneQuestion().please();
      assessmentQuestionInstance = assessmentAttempt.getAssessmentQuestionInstances().get(0);
      answerDTO.setChoiceIndex(0);
    }

    @Test
    void shouldNotBeAbleToAccessNotebookWhenUserHasNoPermission() {
      controller =
          new RestAssessmentController(
              makeMe.modelFactoryService, testabilitySettings, makeMe.aUser().toModelPlease());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.answerQuestion(assessmentQuestionInstance, answerDTO));
    }

    @Test
    void shouldCreateTheAnswerForTheQuestion() throws UnexpectedNoAccessRightException {
      controller.answerQuestion(assessmentQuestionInstance, answerDTO);
      assertThat(assessmentQuestionInstance.getAnswer()).isNotNull();
    }

    @Test
    void shouldNotBeAbleToAnswerTheSameQuestionTwice() {
      AnswerDTO answerDTO1 = new AnswerDTO();
      answerDTO1.setChoiceIndex(0);
      assessmentQuestionInstance.buildAnswer(answerDTO1);
      assertThrows(
          QuestionAnswerException.class,
          () -> controller.answerQuestion(assessmentQuestionInstance, answerDTO));
    }
  }

  @Nested
  class completeAssessmentTest {
    private AssessmentAttempt assessmentAttempt;

    @BeforeEach
    void setup() {
      assessmentAttempt =
          makeMe.anAssessmentAttempt(currentUser.getEntity()).withNQuestions(3).please();
    }

    @Test
    void shouldIncludeTheNotebookCertificateInTheResult() throws UnexpectedNoAccessRightException {
      Notebook notebook = assessmentAttempt.getNotebook();
      makeMe
          .modelFactoryService
          .notebookService(notebook)
          .requestNotebookApproval()
          .approve(makeMe.aTimestamp().please());
      makeMe.refresh(notebook);

      AssessmentAttempt assessmentResult = controller.submitAssessmentResult(assessmentAttempt);

      assertTrue(assessmentResult.isCertifiable());
    }

    @Test
    void shouldReturnAllAnswersCorrect() throws UnexpectedNoAccessRightException {
      assessmentAttempt
          .getAssessmentQuestionInstances()
          .forEach(
              aqi -> {
                Answer answer = new Answer();
                answer.setCorrect(true);
                aqi.setAnswer(answer);
              });

      AssessmentAttempt assessmentResult = controller.submitAssessmentResult(assessmentAttempt);

      assertEquals(3, assessmentResult.getAnswersCorrect());
      assertEquals(3, assessmentResult.getTotalQuestionCount());
    }

    @Test
    void shouldCreateNewAssessmentAttempt() throws UnexpectedNoAccessRightException {
      Timestamp timestamp = makeMe.aTimestamp().please();
      testabilitySettings.timeTravelTo(timestamp);
      controller.submitAssessmentResult(assessmentAttempt);
      AssessmentAttempt assessmentAttempt =
          makeMe.modelFactoryService.assessmentAttemptRepository.findAll().iterator().next();
      assertEquals(timestamp, assessmentAttempt.getSubmittedAt());
    }

    @Test
    void shouldReturnSomeAnswersCorrect() throws UnexpectedNoAccessRightException {
      Answer correctAnswer = new Answer();
      correctAnswer.setCorrect(true);
      Answer wrongAnswer = new Answer();
      wrongAnswer.setCorrect(false);
      assessmentAttempt.getAssessmentQuestionInstances().get(0).setAnswer(correctAnswer);
      assessmentAttempt.getAssessmentQuestionInstances().get(1).setAnswer(correctAnswer);
      assessmentAttempt.getAssessmentQuestionInstances().get(2).setAnswer(wrongAnswer);

      AssessmentAttempt assessmentResult = controller.submitAssessmentResult(assessmentAttempt);

      assertEquals(2, assessmentResult.getAnswersCorrect());
      assertEquals(3, assessmentResult.getTotalQuestionCount());
    }

    @Test
    void shouldNotBeAbleToAccessNotebookWhenUserHasNoPermission() {
      User anotherUser = makeMe.aUser().please();
      assessmentAttempt.setUser(anotherUser);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.submitAssessmentResult(assessmentAttempt));
    }
  }

  @Nested
  class showAssessmentHistoryTest {

    private Notebook notebook;

    @BeforeEach
    void setup() {
      Note topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
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
      makeMe.anAssessmentAttempt(currentUser.getEntity()).notebook(notebook).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertEquals(1, assessmentHistories.size());
    }

    @Test
    void shouldReturnNoAssessmentHistoryForOtherUser() {
      User anotherUser = makeMe.aUser().please();
      makeMe.anAssessmentAttempt(anotherUser).notebook(notebook).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertEquals(0, assessmentHistories.size());
    }

    @Test
    void shouldReturnOnePassAssessmentHistory() {
      makeMe.anAssessmentAttempt(currentUser.getEntity()).notebook(notebook).score(5, 4).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertTrue(assessmentHistories.get(0).getIsPass());
    }

    @Test
    void shouldReturnOneFailAssessmentHistory() {
      makeMe.anAssessmentAttempt(currentUser.getEntity()).notebook(notebook).score(5, 2).please();
      List<AssessmentAttempt> assessmentHistories = controller.getMyAssessments();
      assertFalse(assessmentHistories.get(0).getIsPass());
    }
  }
}
