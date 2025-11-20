package com.odde.doughnut.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.NoteBuilder;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AssessmentServiceTests {
  @Autowired MakeMe makeMe;
  @Autowired EntityPersister entityPersister;
  @Autowired com.odde.doughnut.services.AnswerService answerService;
  private CurrentUser currentUser;
  private AssessmentService service;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = new CurrentUser(makeMe.aUser().please());
    service =
        new AssessmentService(
            makeMe.modelFactoryService, entityPersister, testabilitySettings, answerService);
  }

  @Nested
  class assessmentQuestionOrderTest {
    private Notebook notebook;
    private Note topNote;
    private int representativeNumberOfAttempts = 10;

    Set<Integer> performAssessments(int numberOfAttempts) {
      Set<Integer> questionIds = new HashSet<>();
      for (int i = 0; i < numberOfAttempts; i++) {
        AssessmentAttempt assessment = service.generateAssessment(notebook, currentUser.getUser());
        Integer questionId = assessment.getAssessmentQuestionInstances().get(0).getId();
        questionIds.add(questionId);
      }
      return questionIds;
    }

    @BeforeEach
    void setup() {
      testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.seed, 1));
      topNote =
          makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser.getUser()).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);
    }

    @Test
    void shouldPickRandomNotesForAssessment() {
      makeMe.theNote(topNote).withNChildrenThat(10, NoteBuilder::hasAnApprovedQuestion).please();
      Set<Integer> questionIds = performAssessments(representativeNumberOfAttempts);
      assertTrue(questionIds.size() > 1, "Expected questions from different notes.");
    }

    @Test
    void shouldPickRandomQuestionsFromTheSameNote() {
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
      topNote =
          makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser.getUser()).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(5);
    }

    @Test
    void shouldReturn5QuestionsWhenThereAreMoreThan5NotesWithQuestions() {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnApprovedQuestion).please();
      AssessmentAttempt assessment = service.generateAssessment(notebook, currentUser.getUser());
      assertEquals(5, assessment.getAssessmentQuestionInstances().size());
    }

    @Test
    void shouldPersistTheQuestion() {
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);
      makeMe.theNote(topNote).withNChildrenThat(1, NoteBuilder::hasAnApprovedQuestion).please();
      AssessmentAttempt assessment = service.generateAssessment(notebook, currentUser.getUser());
      assertThat(assessment.getAssessmentQuestionInstances().get(0).getId()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenThereAreNotEnoughQuestions() {
      makeMe.theNote(topNote).withNChildrenThat(4, NoteBuilder::hasAnApprovedQuestion).please();
      assertThrows(
          ApiException.class, () -> service.generateAssessment(notebook, currentUser.getUser()));
    }

    @Test
    void shouldGetOneApprovedQuestionFromEachNoteOnly() {
      makeMe.theNote(topNote).withNChildrenThat(5, NoteBuilder::hasAnUnapprovedQuestion).please();
      assertThrows(
          ApiException.class, () -> service.generateAssessment(notebook, currentUser.getUser()));
    }
  }
}
