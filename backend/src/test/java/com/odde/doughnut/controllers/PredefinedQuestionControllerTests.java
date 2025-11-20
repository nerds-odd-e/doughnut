package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.QuestionSuggestionForFineTuningRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.PredefinedQuestionService;
import com.odde.doughnut.services.SuggestedQuestionForFineTuningService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PredefinedQuestionControllerTests extends ControllerTestBase {
  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  @Autowired QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;
  @Autowired SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired PredefinedQuestionService predefinedQuestionService;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIChatCompletionMock openAIChatCompletionMock;

  PredefinedQuestionController controller;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    currentUser.setUser(makeMe.aUser().please());
    controller =
        new PredefinedQuestionController(
            openAiApi,
            predefinedQuestionService,
            suggestedQuestionForFineTuningService,
            testabilitySettings,
            getTestObjectMapper(),
            authorizationService,
            globalSettingsService);
  }

  PredefinedQuestionController nullUserController() {
    currentUser.setUser(null);
    return new PredefinedQuestionController(
        openAiApi,
        predefinedQuestionService,
        suggestedQuestionForFineTuningService,
        testabilitySettings,
        getTestObjectMapper(),
        authorizationService,
        globalSettingsService);
  }

  private com.fasterxml.jackson.databind.ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class SuggestQuestionForFineTuning {
    PredefinedQuestion predefinedQuestion;
    MCQWithAnswer mcqWithAnswer;
    Note note;

    QuestionSuggestionCreationParams suggestionWithPositiveFeedback =
        new QuestionSuggestionCreationParams("this is a comment", true);

    QuestionSuggestionCreationParams suggestionWithNegativeFeedback =
        new QuestionSuggestionCreationParams("this is a comment", false);

    @BeforeEach
    void setup() throws PredefinedQuestionNotPossibleException {
      note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      predefinedQuestion =
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestion(mcqWithAnswer, note).please();
    }

    @Test
    void suggestQuestionWithAPositiveFeedback() {

      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              predefinedQuestion, suggestionWithPositiveFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          predefinedQuestion.getMcqWithAnswer(),
          suggestedQuestionForFineTuning.getPreservedQuestion());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertTrue(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("0", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithANegativeFeedback() {
      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              predefinedQuestion, suggestionWithNegativeFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          predefinedQuestion.getMcqWithAnswer(),
          suggestedQuestionForFineTuning.getPreservedQuestion());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertFalse(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithSnapshotQuestionStem() {
      var suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              predefinedQuestion, suggestionWithPositiveFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertThat(
          suggestedQuestionForFineTuning
              .getPreservedQuestion()
              .getMultipleChoicesQuestion()
              .getStem(),
          equalTo(mcqWithAnswer.getMultipleChoicesQuestion().getStem()));
    }

    @Test
    void createMarkedQuestionInDatabase() {
      long oldCount = questionSuggestionForFineTuningRepository.count();
      controller.suggestQuestionForFineTuning(predefinedQuestion, suggestionWithPositiveFeedback);
      assertThat(questionSuggestionForFineTuningRepository.count(), equalTo(oldCount + 1));
    }
  }

  @Nested
  class GetListOfPredefinedQuestionForNotebook {
    Note noteWithoutQuestions;
    Note noteWithQuestions;

    @BeforeEach
    void setUp() {
      Note headNote =
          makeMe.aHeadNote("My reading list").creatorAndOwner(currentUser.getUser()).please();
      makeMe.theNote(headNote).withNChildren(10).please();
      noteWithoutQuestions =
          makeMe.aNote("Zen and the Art of Motorcycle Maintenance").under(headNote).please();
      Note lila = makeMe.aNote("Lila").creatorAndOwner(currentUser.getUser()).please();
      noteWithQuestions = makeMe.theNote(lila).hasAnApprovedQuestion().please();
    }

    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getAllQuestionByNote(note));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsNotQuestion() throws UnexpectedNoAccessRightException {
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, hasSize(0));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsOneQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion questionOfNote =
          makeMe.aPredefinedQuestion().approvedQuestionOf(noteWithoutQuestions).please();
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, contains(questionOfNote));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion()
        throws UnexpectedNoAccessRightException {
      makeMe.aPredefinedQuestion().approvedQuestionOf(noteWithQuestions).please();
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithQuestions);
      assertThat(results, hasSize(2));
    }
  }

  @Nested
  class addQuestionToNote {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      controller.addQuestionManually(note, mcqWithAnswer);
      makeMe.refresh(note);
      assertThat(note.getPredefinedQuestions(), hasSize(1));
    }
  }

  @Nested
  class RefineQuestion {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void givenQuestion_thenReturnRefineQuestion() throws UnexpectedNoAccessRightException {
      // Setup
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      PredefinedQuestion predefinedQuestion = makeMe.aPredefinedQuestion().please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(mcqWithAnswer);

      // Execute & Verify
      PredefinedQuestion result = controller.refineQuestion(note, predefinedQuestion);
      assertThat(result.getMcqWithAnswer(), equalTo(mcqWithAnswer));
    }

    @Test
    void refineQuestionFailedWithGpt35WillNotTryAgain() throws JsonProcessingException {
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCallJsonNode(
          new ObjectMapperConfig()
              .objectMapper()
              .readTree(
                  "{\"multipleChoicesQuestion\":{\"stem\":null,\"choices\":null},\"correctChoiceIndex\":0,\"approve\":false}"),
          "");
      assertThrows(RuntimeException.class, () -> controller.refineQuestion(note, mcqWithAnswer));
      verify(openAiApi, Mockito.times(1)).createChatCompletion(any());
    }
  }

  @Nested
  class ApproveQuestion {
    Note subjectNote;

    @BeforeEach
    void setUp() {
      subjectNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void mustNotBeAbleToApproveOtherPeoplesNoteQuestion() {
      Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedQuestionOf(note).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.toggleApproval(predefinedQuestion));
    }

    @Test
    void approveQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedQuestionOf(subjectNote).please();
      predefinedQuestion.setApproved(false);
      PredefinedQuestion approvedQuestion = controller.toggleApproval(predefinedQuestion);
      assertTrue(approvedQuestion.isApproved());
    }

    @Test
    void unApproveQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedQuestionOf(subjectNote).please();
      predefinedQuestion.setApproved(true);
      PredefinedQuestion approvedQuestion = controller.toggleApproval(predefinedQuestion);
      assertFalse(approvedQuestion.isApproved());
    }
  }

  @Nested
  class ExportQuestionGeneration {
    Note note;

    @BeforeEach
    void setup() {
      note =
          makeMe
              .aNote()
              .creatorAndOwner(currentUser.getUser())
              .titleConstructor("There are 42 prefectures in Japan")
              .please();
    }

    @Test
    void shouldNotBeAbleToExportQuestionGenerationForNoteIAmNotAuthorized() {
      Note otherNote = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.exportQuestionGeneration(otherNote));
    }

    @Test
    void shouldExportQuestionGenerationWithChatCompletionRequest()
        throws UnexpectedNoAccessRightException {
      com.theokanning.openai.completion.chat.ChatCompletionRequest request =
          controller.exportQuestionGeneration(note);
      assertThat(request.getModel(), notNullValue());
      assertThat(request.getMessages(), notNullValue());
    }
  }
}
