package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.client.OpenAiApi;
import java.util.*;
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

  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  private Notebook notebook;
  private Note topNote;
  private RestAssessmentController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, userModel);
  }

  @Nested
  class generateOnlineAssessmentTest {
    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(userModel).please();
      notebook = topNote.getNotebook();
    }

    @Test
    void whenNotLogin() {
      userModel = modelFactoryService.toUserModel(null);
      controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, userModel);
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
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      makeMe.theNote(note).withNChildren(6);
      makeMe.refresh(note);
      for (Note n : note.getChildren()) {
        makeMe.aQuestion().spellingQuestionOfNote(n).approveQuestion().please();
      }
      makeMe.refresh(note.getNotebook());
      BazaarNotebook bazaarNotebook = makeMe.aBazaarNotebook(note.getNotebook()).please();
      List<QuizQuestion> assessment =
          controller.generateAssessmentQuestions(bazaarNotebook.getNotebook());
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldReturn5ApprovedQuestionsWhenThereAreMoreThan5NotesWithQuestions()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildren(10).please();
      makeMe.refresh(topNote);
      for (Note note : topNote.getChildren()) {
        if (note.getId() % 2 == 0) {
          makeMe.aQuestion().spellingQuestionOfNote(note).approveQuestion().please();
        } else {
          makeMe.aQuestion().spellingQuestionOfNote(note).please();
        }
      }
      makeMe.refresh(notebook);

      List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);

      assertEquals(assessment.size(), 5);
      assertEquals(assessment.stream().filter(x -> x.approved).count(), 5);
    }

    @Test
    void shouldThrowExceptionWhenThereAreNotEnoughApprovedQuestions() {
      makeMe.theNote(topNote).withNChildren(10).please();
      makeMe.refresh(topNote);
      for (Note note : topNote.getChildren()) {
        makeMe.aQuestion().spellingQuestionOfNote(note).please();
      }
      makeMe.refresh(notebook);

      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }

    @Test
    void shouldReturn5ApprovedQuestionsFromDifferentNotesWhenThereAreMoreThan5NotesWithQuestions()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildren(10).please();
      makeMe.refresh(topNote);
      for (Note note : topNote.getChildren()) {
        if (note.getId() % 2 == 0) {
          makeMe.aQuestion().spellingQuestionOfNote(note).approveQuestion().please();
        } else {
          makeMe.aQuestion().spellingQuestionOfNote(note).please();
        }
      }
      makeMe.refresh(notebook);

      List<QuizQuestion> assessment = controller.generateAssessmentQuestions(notebook);

      Map<Integer, QuizQuestion> distinctMap = new HashMap<>();
      for (QuizQuestion question : assessment) {
        distinctMap.putIfAbsent(question.getNote().getId(), question);
      }

      assertEquals(distinctMap.values().size(), 5);
      assertEquals(assessment.size(), 5);
    }

    @Test
    void shouldThrowExceptionWhenThereAreNotEnoughUniqueQuestions() {
      makeMe.theNote(topNote).withNChildren(3).please();
      makeMe.refresh(topNote);
      for (Note note : topNote.getChildren()) {
        makeMe.aQuestion().spellingQuestionOfNote(note).approveQuestion().please();
        makeMe.aQuestion().spellingQuestionOfNote(note).approveQuestion().please();
        makeMe.aQuestion().spellingQuestionOfNote(note).approveQuestion().please();
      }
      makeMe.refresh(notebook);

      assertThrows(ApiException.class, () -> controller.generateAssessmentQuestions(notebook));
    }
  }
}
