package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssessmentResult;
import com.odde.doughnut.controllers.dto.NoteIdAndTitle;
import com.odde.doughnut.controllers.dto.QuestionAnswerPair;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.theokanning.openai.client.OpenAiApi;
import java.util.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAssessmentController(openAiApi, makeMe.modelFactoryService, currentUser);
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
      int numberOfQuestions = 10;

      Consumer<NoteBuilder> multipleApprovedQuestionsForNote =
          noteBuilder -> noteBuilder.hasApprovedQuestions(numberOfQuestions);

      makeMe.theNote(topNote).withNChildrenThat(1, multipleApprovedQuestionsForNote).please();

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
              openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModelPlease());
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
  }

  @Nested
  class completeAssessmentTest {
    private Notebook notebook;
    private Note topNote;
    private List<QuestionAnswerPair> questionsAnswerPairs;
    AssessmentResult expectedAssessmentResult = new AssessmentResult();

    @BeforeEach
    void setup() {
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();

      makeMe.theNote(topNote).withNChildrenThat(2, NoteBuilder::hasAnApprovedQuestion).please();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(2);
      questionsAnswerPairs = new ArrayList<>();

      for (Note note : notebook.getNotes()) {
        QuizQuestionAndAnswer quizQuestionAndAnswer = note.getQuizQuestionAndAnswers().getFirst();
        QuestionAnswerPair questionAnswerPair = new QuestionAnswerPair();
        questionAnswerPair.setQuestionId(quizQuestionAndAnswer.getId());
        quizQuestionAndAnswer.setCorrectAnswerIndex(1);
        questionAnswerPair.setAnswerId(0);
        questionsAnswerPairs.add(questionAnswerPair);

        NoteIdAndTitle noteIdAndTitle = new NoteIdAndTitle();
        noteIdAndTitle.setId(note.getId());
        noteIdAndTitle.setTitle(note.getNoteTopic().getTopicConstructor());
        expectedAssessmentResult.setNoteIdAndTitles(new NoteIdAndTitle[] {noteIdAndTitle});
      }
    }

    @Test
    void submitAssessmentResultCheckScore() throws UnexpectedNoAccessRightException {
      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, questionsAnswerPairs);

      assertEquals(questionsAnswerPairs.size(), assessmentResult.getTotalCount());
      assertEquals(0, assessmentResult.getCorrectCount());
    }

    @Disabled
    @Test
    void submitAssessmentResultCheckNoteIdAndTitles() throws UnexpectedNoAccessRightException {
      AssessmentResult assessmentResult =
          controller.submitAssessmentResult(notebook, questionsAnswerPairs);

      assertEquals(questionsAnswerPairs.size(), assessmentResult.getNoteIdAndTitles().length, "Expected number of notes do not match the provided number of questions.");
      for( int i = 0; i < questionsAnswerPairs.size(); i++){
        NoteIdAndTitle expectedNoteIdAndTitle = expectedAssessmentResult.getNoteIdAndTitles()[i];
        NoteIdAndTitle providedNoteIdAndTitle = assessmentResult.getNoteIdAndTitles()[i];
        assertEquals(expectedNoteIdAndTitle.getId(), providedNoteIdAndTitle.getId());
        assertEquals(expectedNoteIdAndTitle.getTitle(), providedNoteIdAndTitle.getTitle());
      }
    }
  }

  @Nested
  class assessmentHistoryTest {
    @Test
    void shouldReturnAssessmentHistory() throws UnexpectedNoAccessRightException {
      var topNote = makeMe.aHeadNote("Countries").creatorAndOwner(currentUser).please();
      var notebook = topNote.getNotebook();

      controller.submitAssessmentResult(notebook, new ArrayList<>());

      List<AssessmentAttempt> assessmentHistory = controller.getAssessmentHistory();
      assertEquals(1, assessmentHistory.size());
      assertEquals(
          "Countries",
          assessmentHistory.stream()
              .findFirst()
              .get()
              .getNotebook()
              .getHeadNote()
              .getNoteTopic()
              .getTopicConstructor());
      assertEquals(0, assessmentHistory.stream().findFirst().get().getAnswersCorrect());
      assertEquals(1, assessmentHistory.stream().findFirst().get().getAnswersTotal());
    }
  }
}
