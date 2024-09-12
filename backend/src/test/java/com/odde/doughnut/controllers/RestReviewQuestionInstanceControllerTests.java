package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.sql.Timestamp;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestReviewQuestionInstanceControllerTests {
  @Mock OpenAiApi openAiApi;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIChatCompletionMock openAIChatCompletionMock;

  RestReviewQuestionController controller;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestReviewQuestionController(
            openAiApi, modelFactoryService, currentUser, testabilitySettings);
  }

  RestReviewQuestionController nullUserController() {
    return new RestReviewQuestionController(
        openAiApi, modelFactoryService, makeMe.aNullUserModelPlease(), testabilitySettings);
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    ReviewQuestionInstance reviewQuestionInstance;
    AnswerDTO answerDTO = new AnswerDTO();

    @BeforeEach
    void setup() {
      Note answerNote = makeMe.aNote().rememberSpelling().please();
      reviewPoint =
          makeMe
              .aReviewPointFor(answerNote)
              .by(currentUser)
              .forgettingCurveAndNextReviewAt(200)
              .please();
      reviewQuestionInstance =
          makeMe.aReviewQuestionInstance().approvedSpellingQuestionOf(answerNote).please();
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnsweredQuestion answerResult = controller.answerQuiz(reviewQuestionInstance, answerDTO);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(reviewPoint.getLastReviewedAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(reviewQuestionInstance, answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(reviewQuestionInstance, answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          reviewPoint.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(reviewQuestionInstance, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        reviewQuestionInstance =
            makeMe
                .aReviewQuestionInstance()
                .approvedSpellingQuestionOf(reviewPoint.getNote())
                .please();
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
        AnsweredQuestion answerResult = controller.answerQuiz(reviewQuestionInstance, answerDTO);
        assertFalse(answerResult.correct);
        assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastReviewedAtTime() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Timestamp lastReviewedAt = reviewPoint.getLastReviewedAt();
        Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
        controller.answerQuiz(reviewQuestionInstance, answerDTO);
        assertThat(reviewPoint.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(reviewPoint.getLastReviewedAt(), equalTo(lastReviewedAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(reviewQuestionInstance, answerDTO);
        assertThat(
            reviewPoint.getNextReviewAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
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
      note = makeMe.aNote().creatorAndOwner(currentUser).please();
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
      long oldCount = modelFactoryService.questionSuggestionForFineTuningRepository.count();
      controller.suggestQuestionForFineTuning(predefinedQuestion, suggestionWithPositiveFeedback);
      assertThat(
          modelFactoryService.questionSuggestionForFineTuningRepository.count(),
          equalTo(oldCount + 1));
    }
  }

  @Nested
  class GenerateQuestion {
    MCQWithAnswer jsonQuestion;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();
      jsonQuestion =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctChoiceIndex(0)
              .please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestReviewQuestionController restAiController =
                new RestReviewQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.generateQuestion(note);
          });
    }

    @Test
    void createQuizQuestion() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      ReviewQuestionInstance reviewQuestionInstance = controller.generateQuestion(note);

      Assertions.assertThat(
              reviewQuestionInstance.getBareQuestion().getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
    }

    @Test
    void createQuizQuestionFailedWithGpt35WillNotTryAgain() throws JsonProcessingException {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCallJsonNode(
          new ObjectMapper().readTree("{\"stem\": \"\"}"), "");
      assertThat(controller.generateQuestion(note), nullValue());
      verify(openAiApi, Mockito.times(1)).createChatCompletion(any());
    }

    @Test
    void mustUseTheRightModel() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .globalSettingQuestionGeneration()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      controller.generateQuestion(note);
      ArgumentCaptor<ChatCompletionRequest> captor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletion(captor.capture());
      assertThat(captor.getValue().getModel(), equalTo("gpt-new"));
    }

    @Test
    void generateQuestionForAssessmentOfNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestReviewQuestionController restAiController =
                new RestReviewQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.generateAIQuestionWithoutSave(note);
          });
    }

    @Test
    void generateQuestionForAssessmentOfNote() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      PredefinedQuestion quizQuestionDTO = controller.generateAIQuestionWithoutSave(note);

      Assertions.assertThat(
              quizQuestionDTO.getBareQuestion().getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
      Assertions.assertThat(quizQuestionDTO.getCorrectAnswerIndex()).isEqualTo(0);
    }
  }

  @Nested
  class RegenerateQuestion {
    ReviewQuestionInstance reviewQuestionInstance;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();

      reviewQuestionInstance =
          makeMe.aReviewQuestionInstance().approvedSpellingQuestionOf(note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestReviewQuestionController restAiController =
                new RestReviewQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.regenerate(reviewQuestionInstance);
          });
    }

    @Test
    void createQuizQuestion() {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      ReviewQuestionInstance reviewQuestionInstance =
          controller.regenerate(this.reviewQuestionInstance);

      Assertions.assertThat(
              reviewQuestionInstance.getBareQuestion().getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
    }
  }

  @Nested
  class Contest {
    ReviewQuestionInstance reviewQuestionInstance;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.comment = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion = makeMe.aMCQWithAnswer().please();
      Note note = makeMe.aNote().please();
      reviewQuestionInstance =
          makeMe
              .aReviewQuestionInstance()
              .ofAIGeneratedQuestion(aiGeneratedQuestion, note)
              .please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestReviewQuestionController restAiController =
                new RestReviewQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.contest(reviewQuestionInstance);
          });
    }

    @Test
    void rejected() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      ReviewQuestionContestResult contest = controller.contest(reviewQuestionInstance);
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      controller.contest(reviewQuestionInstance);
      ArgumentCaptor<ChatCompletionRequest> argumentCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi, times(1)).createChatCompletion(argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getModel(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      ReviewQuestionContestResult contest = controller.contest(reviewQuestionInstance);
      assertFalse(contest.rejected);
    }
  }

  @Nested
  class GetListOfPredefinedQuestionForNotebook {
    Note noteWithoutQuestions;
    Note noteWithQuestions;

    @BeforeEach
    void setUp() {
      Note headNote = makeMe.aHeadNote("My reading list").creatorAndOwner(currentUser).please();
      makeMe.theNote(headNote).withNChildren(10).please();
      noteWithoutQuestions =
          makeMe.aNote("Zen and the Art of Motorcycle Maintenance").under(headNote).please();
      Note lila = makeMe.aNote("Lila").creatorAndOwner(currentUser).please();
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
          makeMe.aPredefinedQuestion().approvedSpellingQuestionOf(noteWithoutQuestions).please();
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, contains(questionOfNote));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion()
        throws UnexpectedNoAccessRightException {
      makeMe.aPredefinedQuestion().approvedSpellingQuestionOf(noteWithQuestions).please();
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
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
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
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      PredefinedQuestion predefinedQuestion = makeMe.aPredefinedQuestion().please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(mcqWithAnswer, "");
      PredefinedQuestion result = controller.refineQuestion(note, predefinedQuestion);

      assertEquals(0, result.getCorrectAnswerIndex());
      assertEquals(
          "a default question stem",
          result.getBareQuestion().getMultipleChoicesQuestion().getStem());
      assertEquals(
          List.of("choice1", "choice2", "choice3"),
          result.getBareQuestion().getMultipleChoicesQuestion().getChoices());
    }

    @Test
    void refineQuestionFailedWithGpt35WillNotTryAgain() throws JsonProcessingException {
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCallJsonNode(
          new ObjectMapper()
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
      subjectNote = makeMe.aNote().creatorAndOwner(currentUser).please();
    }

    @Test
    void mustNotBeAbleToApproveOtherPeoplesNoteQuestion() {
      Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedSpellingQuestionOf(note).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.toggleApproval(predefinedQuestion));
    }

    @Test
    void approveQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedSpellingQuestionOf(subjectNote).please();
      predefinedQuestion.setApproved(false);
      PredefinedQuestion approvedQuestion = controller.toggleApproval(predefinedQuestion);
      assertTrue(approvedQuestion.isApproved());
    }

    @Test
    void unApproveQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion predefinedQuestion =
          makeMe.aPredefinedQuestion().approvedSpellingQuestionOf(subjectNote).please();
      predefinedQuestion.setApproved(true);
      PredefinedQuestion approvedQuestion = controller.toggleApproval(predefinedQuestion);
      assertFalse(approvedQuestion.isApproved());
    }
  }
}
