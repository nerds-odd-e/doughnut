package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
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
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    userModel = makeMe.aUser().toModelPlease();
    controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, userModel);
  }

  @Nested
  class generateAssessmentTest {
    @BeforeEach
    void setup() {
      MCQWithAnswer jsonQuestion =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctChoiceIndex(0)
              .please();
      topNote = makeMe.aNote().creatorAndOwner(userModel).please();
      notebook = topNote.getNotebook();
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
    }

    @Test
    void whenNotLogin() {
      userModel = modelFactoryService.toUserModel(null);
      controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, userModel);
      assertThrows(ResponseStatusException.class, () -> controller.generateAiQuestions(notebook));
    }

    @Test
    void shouldBeAbleToAccessNotebookThatIsInTheBazaar() throws UnexpectedNoAccessRightException {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      makeMe.theNote(note).withNChildren(4);
      makeMe.refresh(note);
      BazaarNotebook bazaarNotebook = makeMe.aBazaarNotebook(note.getNotebook()).please();
      List<QuizQuestion> assessment = controller.generateAiQuestions(bazaarNotebook.getNotebook());
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldNotBeAbleToAccessNotebookThatIsNotInTheBazaar() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.generateAiQuestions(note.getNotebook()));
    }

    @Test
    void shouldReturn5QuestionsWhenThereAre5Notes() throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildren(4);
      makeMe.refresh(topNote);
      List<QuizQuestion> assessment = controller.generateAiQuestions(notebook);
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldReturn5QuestionsWhenThereAreMoreThan5Notes()
        throws UnexpectedNoAccessRightException {
      makeMe.theNote(topNote).withNChildren(10);
      makeMe.refresh(topNote);
      List<QuizQuestion> assessment = controller.generateAiQuestions(notebook);
      assertEquals(5, assessment.size());
    }

    @Test
    void shouldThrowErrorWhenThereAreLessThan5Notes() {
      makeMe.theNote(topNote).withNChildren(3);
      makeMe.refresh(topNote);
      assertThrows(ResponseStatusException.class, () -> controller.generateAiQuestions(notebook));
    }
  }
}