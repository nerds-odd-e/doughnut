package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionAndAnswerUpdateDTO;
import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.controllers.dto.QuizQuestionContestResult;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.QuizQuestionBuilder;
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
class RestQuizQuestionAndAnswerControllerTests {
  @Mock OpenAiApi openAiApi;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIChatCompletionMock openAIChatCompletionMock;

  RestQuizQuestionController controller;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestQuizQuestionController(
            openAiApi, modelFactoryService, currentUser, testabilitySettings);
  }

  RestQuizQuestionController nullUserController() {
    return new RestQuizQuestionController(
        openAiApi, modelFactoryService, makeMe.aNullUserModelPlease(), testabilitySettings);
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    QuizQuestionAndAnswer quizQuestionAndAnswer;
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
      QuizQuestionBuilder quizQuestionBuilder = makeMe.aQuestion();
      quizQuestionAndAnswer = quizQuestionBuilder.approvedSpellingQuestionOf(answerNote).please();
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnsweredQuestion answerResult =
          controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(reviewPoint.getLastReviewedAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          reviewPoint.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        QuizQuestionBuilder quizQuestionBuilder = makeMe.aQuestion();
        quizQuestionAndAnswer =
            quizQuestionBuilder.approvedSpellingQuestionOf(reviewPoint.getNote()).please();
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
        AnsweredQuestion answerResult =
            controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
        assertFalse(answerResult.correct);
        assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastReviewedAtTime() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Timestamp lastReviewedAt = reviewPoint.getLastReviewedAt();
        Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
        controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
        assertThat(reviewPoint.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(reviewPoint.getLastReviewedAt(), equalTo(lastReviewedAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(quizQuestionAndAnswer.getQuizQuestion(), answerDTO);
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
    QuizQuestionAndAnswer quizQuestionAndAnswer;
    MCQWithAnswer mcqWithAnswer;
    Note note;

    QuestionSuggestionCreationParams suggestionWithPositiveFeedback =
        new QuestionSuggestionCreationParams("this is a comment", true);

    QuestionSuggestionCreationParams suggestionWithNegativeFeedback =
        new QuestionSuggestionCreationParams("this is a comment", false);

    @BeforeEach
    void setup() throws QuizQuestionNotPossibleException {
      note = makeMe.aNote().creatorAndOwner(currentUser).please();
      mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      quizQuestionAndAnswer =
          makeMe.aQuestion().ofAIGeneratedQuestion(mcqWithAnswer, note).please();
    }

    @Test
    void suggestQuestionWithAPositiveFeedback() {

      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              quizQuestionAndAnswer, suggestionWithPositiveFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          quizQuestionAndAnswer.getMcqWithAnswer(),
          suggestedQuestionForFineTuning.getPreservedQuestion());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertTrue(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("0", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithANegativeFeedback() {
      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              quizQuestionAndAnswer, suggestionWithNegativeFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          quizQuestionAndAnswer.getMcqWithAnswer(),
          suggestedQuestionForFineTuning.getPreservedQuestion());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertFalse(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithSnapshotQuestionStem() {
      var suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(
              quizQuestionAndAnswer, suggestionWithPositiveFeedback);
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
      controller.suggestQuestionForFineTuning(
          quizQuestionAndAnswer, suggestionWithPositiveFeedback);
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
            RestQuizQuestionController restAiController =
                new RestQuizQuestionController(
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
      QuizQuestionInNotebook quizQuestion = controller.generateQuestion(note);

      Assertions.assertThat(quizQuestion.getQuizQuestion().getMultipleChoicesQuestion().getStem())
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
            RestQuizQuestionController restAiController =
                new RestQuizQuestionController(
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
      QuizQuestionAndAnswer quizQuestionDTO = controller.generateAIQuestionWithoutSave(note);

      Assertions.assertThat(
              quizQuestionDTO.getQuizQuestion().getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
      Assertions.assertThat(quizQuestionDTO.getCorrectAnswerIndex()).isEqualTo(0);
    }
  }

  @Nested
  class RegenerateQuestion {
    QuizQuestionAndAnswer quizQuestionAndAnswer;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();

      quizQuestionAndAnswer = makeMe.aQuestion().approvedSpellingQuestionOf(note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestQuizQuestionController restAiController =
                new RestQuizQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.regenerate(quizQuestionAndAnswer.getQuizQuestion());
          });
    }

    @Test
    void createQuizQuestion() {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      QuizQuestion quizQuestion =
          controller.regenerate(this.quizQuestionAndAnswer.getQuizQuestion());

      Assertions.assertThat(quizQuestion.getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
    }
  }

  @Nested
  class Contest {
    QuizQuestionAndAnswer quizQuestion;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.comment = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion = makeMe.aMCQWithAnswer().please();
      Note note = makeMe.aNote().please();
      quizQuestion = makeMe.aQuestion().ofAIGeneratedQuestion(aiGeneratedQuestion, note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestQuizQuestionController restAiController =
                new RestQuizQuestionController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.contest(quizQuestion.getQuizQuestion());
          });
    }

    @Test
    void rejected() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      QuizQuestionContestResult contest = controller.contest(quizQuestion.getQuizQuestion());
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      controller.contest(quizQuestion.getQuizQuestion());
      ArgumentCaptor<ChatCompletionRequest> argumentCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi, times(1)).createChatCompletion(argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getModel(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      QuizQuestionContestResult contest = controller.contest(quizQuestion.getQuizQuestion());
      assertFalse(contest.rejected);
    }
  }

  @Nested
  class GetListOfQuizQuestionAndAnswerForNotebook {
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
      List<QuizQuestionAndAnswer> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, hasSize(0));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsOneQuestion() throws UnexpectedNoAccessRightException {
      QuizQuestionAndAnswer questionOfNote =
          makeMe.aQuestion().approvedSpellingQuestionOf(noteWithoutQuestions).please();
      List<QuizQuestionAndAnswer> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, contains(questionOfNote));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion()
        throws UnexpectedNoAccessRightException {
      makeMe.aQuestion().approvedSpellingQuestionOf(noteWithQuestions).please();
      List<QuizQuestionAndAnswer> results = controller.getAllQuestionByNote(noteWithQuestions);
      assertThat(results, hasSize(2));
    }
  }

  @Nested
  class addQuestionToNote {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
      controller.addQuestionManually(note, mcqWithAnswer);
      makeMe.refresh(note);
      assertThat(note.getQuizQuestionAndAnswers(), hasSize(1));
    }
  }

  @Nested
  class deleteQuestionFromNote {
    @Test
    void authorization() {
      QuizQuestionAndAnswer questionAndAnswer = makeMe.aQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteQuestion(questionAndAnswer.getNote(), questionAndAnswer));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
      mcqWithAnswer.setNote(note);
      controller.deleteQuestion(note, mcqWithAnswer);
      makeMe.refresh(note);
      assertThat(note.getQuizQuestionAndAnswers(), hasSize(0));
    }
  }

  @Nested
  class editQuestionInNote {
    @Test
    void authorization() {
      QuizQuestionAndAnswer questionAndAnswer = makeMe.aQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () ->
              controller.editQuestion(
                  questionAndAnswer.getNote(),
                  questionAndAnswer,
                  new QuestionAndAnswerUpdateDTO()));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
      mcqWithAnswer.setNote(note);
      mcqWithAnswer.setCorrectAnswerIndex(0);

      QuestionAndAnswerUpdateDTO questionAndAnswer = new QuestionAndAnswerUpdateDTO();
      questionAndAnswer.setCorrectAnswerIndex(1);
      controller.editQuestion(note, mcqWithAnswer, questionAndAnswer);
      makeMe.refresh(mcqWithAnswer);
      assertThat(mcqWithAnswer.getCorrectAnswerIndex(), equalTo(1));
    }
  }

  @Nested
  class RefineQuestion {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void givenQuestion_thenReturnRefineQuestion() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      QuizQuestionAndAnswer questionAndAnswer = makeMe.aQuestion().please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(mcqWithAnswer, "");
      QuizQuestionAndAnswer result = controller.refineQuestion(note, questionAndAnswer);

      assertEquals(0, result.getCorrectAnswerIndex());
      assertEquals(
          "a default question stem",
          result.getQuizQuestion().getMultipleChoicesQuestion().getStem());
      assertEquals(
          List.of("choice1", "choice2", "choice3"),
          result.getQuizQuestion().getMultipleChoicesQuestion().getChoices());
    }

    @Test
    void refineQuestionFailedWithGpt35WillNotTryAgain() throws JsonProcessingException {
      QuizQuestionAndAnswer mcqWithAnswer = makeMe.aQuestion().please();
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
      QuizQuestionAndAnswer quizQuestionAndAnswer =
          makeMe.aQuestion().approvedSpellingQuestionOf(note).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.toggleApproval(quizQuestionAndAnswer));
    }

    @Test
    void approveQuestion() throws UnexpectedNoAccessRightException {
      QuizQuestionAndAnswer quizQuestionAndAnswer =
          makeMe.aQuestion().approvedSpellingQuestionOf(subjectNote).please();
      quizQuestionAndAnswer.setApproved(false);
      QuizQuestionAndAnswer approvedQuestion = controller.toggleApproval(quizQuestionAndAnswer);
      assertTrue(approvedQuestion.isApproved());
    }

    @Test
    void unApproveQuestion() throws UnexpectedNoAccessRightException {
      QuizQuestionAndAnswer quizQuestionAndAnswer =
          makeMe.aQuestion().approvedSpellingQuestionOf(subjectNote).please();
      quizQuestionAndAnswer.setApproved(true);
      QuizQuestionAndAnswer approvedQuestion = controller.toggleApproval(quizQuestionAndAnswer);
      assertFalse(approvedQuestion.isApproved());
    }
  }
}
