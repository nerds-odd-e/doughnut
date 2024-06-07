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
import com.odde.doughnut.controllers.dto.QuizQuestionContestResult;
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
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
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
class RestQuizQuestionControllerTests {
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
        openAiApi, modelFactoryService, makeMe.aNullUserModel(), testabilitySettings);
  }

  @Nested
  class answer {
    ReviewPoint reviewPoint;
    QuizQuestion quizQuestion;
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
      quizQuestion = makeMe.aQuestion().spellingQuestionOfReviewPoint(answerNote).please();
      answerDTO.setSpellingAnswer(answerNote.getTopicConstructor());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateReviewPoint() {
      Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
      AnsweredQuestion answerResult = controller.answerQuiz(quizQuestion, answerDTO);
      assertTrue(answerResult.correct);
      assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(reviewPoint.getLastReviewedAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestion, answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
      Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
      controller.answerQuiz(quizQuestion, answerDTO);
      assertThat(reviewPoint.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          reviewPoint.getLastReviewedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(quizQuestion, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        quizQuestion =
            makeMe.aQuestion().spellingQuestionOfReviewPoint(reviewPoint.getNote()).please();
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Integer oldRepetitionCount = reviewPoint.getRepetitionCount();
        AnsweredQuestion answerResult = controller.answerQuiz(quizQuestion, answerDTO);
        assertFalse(answerResult.correct);
        assertThat(reviewPoint.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastReviewedAtTime() {
        testabilitySettings.timeTravelTo(reviewPoint.getNextReviewAt());
        Timestamp lastReviewedAt = reviewPoint.getLastReviewedAt();
        Integer oldForgettingCurveIndex = reviewPoint.getForgettingCurveIndex();
        controller.answerQuiz(quizQuestion, answerDTO);
        assertThat(reviewPoint.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(reviewPoint.getLastReviewedAt(), equalTo(lastReviewedAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(quizQuestion, answerDTO);
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
    QuizQuestion quizQuestion;
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
      quizQuestion = makeMe.aQuestion().ofAIGeneratedQuestion(mcqWithAnswer, note).please();
    }

    @Test
    void suggestQuestionWithAPositiveFeedback() {

      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(quizQuestion, suggestionWithPositiveFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          quizQuestion.getMcqWithAnswer().toJsonString(),
          suggestedQuestionForFineTuning.getPreservedQuestion().toJsonString());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertTrue(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("0", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithANegativeFeedback() {
      SuggestedQuestionForFineTuning suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(quizQuestion, suggestionWithNegativeFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertEquals(
          quizQuestion.getMcqWithAnswer().toJsonString(),
          suggestedQuestionForFineTuning.getPreservedQuestion().toJsonString());
      assertEquals("this is a comment", suggestedQuestionForFineTuning.getComment());
      assertFalse(suggestedQuestionForFineTuning.isPositiveFeedback(), "Incorrect Feedback");
      assertEquals("", suggestedQuestionForFineTuning.getRealCorrectAnswers());
    }

    @Test
    void suggestQuestionWithSnapshotQuestionStem() {
      var suggestedQuestionForFineTuning =
          controller.suggestQuestionForFineTuning(quizQuestion, suggestionWithPositiveFeedback);
      assert suggestedQuestionForFineTuning != null;
      assertThat(
          suggestedQuestionForFineTuning.getPreservedQuestion().stem, equalTo(mcqWithAnswer.stem));
    }

    @Test
    void createMarkedQuestionInDatabase() {
      long oldCount = modelFactoryService.questionSuggestionForFineTuningRepository.count();
      controller.suggestQuestionForFineTuning(quizQuestion, suggestionWithPositiveFeedback);
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
                    makeMe.aNullUserModel(),
                    testabilitySettings);
            restAiController.generateQuestion(note);
          });
    }

    @Test
    void createQuizQuestion() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      QuizQuestion quizQuestion = controller.generateQuestion(note);

      Assertions.assertThat(quizQuestion.getMultipleChoicesQuestion().stem)
          .contains("What is the first color in the rainbow?");
    }

    @Test
    void createQuizQuestionFailedWithGpt35WillNotTryAgain() throws JsonProcessingException {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCallJsonNode(
          new ObjectMapper().readTree("{\"stem\": \"\"}"), "");
      assertThrows(ResponseStatusException.class, () -> controller.generateQuestion(note));
      verify(openAiApi, Mockito.times(1)).createChatCompletion(any());
    }

    @Test
    void mustUseTheRightModel() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .getGlobalSettingQuestionGeneration()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      controller.generateQuestion(note);
      ArgumentCaptor<ChatCompletionRequest> captor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi).createChatCompletion(captor.capture());
      assertThat(captor.getValue().getModel(), equalTo("gpt-new"));
    }
  }

  @Nested
  class RegenerateQuestion {
    QuizQuestion quizQuestion;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();

      quizQuestion = makeMe.aQuestion().spellingQuestionOfNote(note).please();
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
                    makeMe.aNullUserModel(),
                    testabilitySettings);
            restAiController.regenerate(quizQuestion);
          });
    }

    @Test
    void createQuizQuestion() {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(jsonQuestion, "");
      QuizQuestion quizQuestion = controller.regenerate(this.quizQuestion);

      Assertions.assertThat(quizQuestion.getMultipleChoicesQuestion().stem)
          .contains("What is the first color in the rainbow?");
    }
  }

  @Nested
  class Contest {
    QuizQuestion quizQuestion;
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
                    makeMe.aNullUserModel(),
                    testabilitySettings);
            restAiController.contest(quizQuestion);
          });
    }

    @Test
    void rejected() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      QuizQuestionContestResult contest = controller.contest(quizQuestion);
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .getGlobalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      controller.contest(quizQuestion);
      ArgumentCaptor<ChatCompletionRequest> argumentCaptor =
          ArgumentCaptor.forClass(ChatCompletionRequest.class);
      verify(openAiApi, times(1)).createChatCompletion(argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getModel(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(questionEvaluation, "");
      QuizQuestionContestResult contest = controller.contest(quizQuestion);
      assertFalse(contest.rejected);
    }
  }

  @Nested
  class GetListOfQuizQuestionForNoteBook {
    List<QuizQuestion> quizQuestionList = new ArrayList<>();
    Note headNote1;
    Note headNote2;
    Note noteWithoutQuestions;
    Note noteWithQuestions;

    @BeforeEach
    void setUp() {
      headNote1 = makeMe.aHeadNote("headNote1").creatorAndOwner(currentUser).please();
      makeMe.theNote(headNote1).withNChildren(10).please();

      headNote2 = makeMe.aHeadNote("headNote2").creatorAndOwner(currentUser).please();
      makeMe.theNote(headNote2).withNChildren(20).please();

      makeMe.refresh(headNote1);
      makeMe.refresh(headNote2);

      noteWithoutQuestions = makeMe.aNote("a note").under(headNote1).please();
      noteWithQuestions = makeMe.aNote("a note with questions").please();
      makeMe.aQuestion().spellingQuestionOfNote(noteWithQuestions).please();
    }

    @Test
    void getEmptyListOfQuizQuestions() {
      List<QuizQuestion> results = controller.getAllQuizQuestionByNoteBook(headNote1);
      assertThat(results, hasSize(0));
    }

    @Test
    void getListOfQuizQuestions() {
      for (Note note : headNote1.getChildren()) {
        quizQuestionList.add(makeMe.aQuestion().spellingQuestionOfNote(note).please());
      }
      for (Note note : headNote2.getChildren()) {
        makeMe.aQuestion().spellingQuestionOfNote(note).please();
      }
      makeMe.refresh(headNote1);
      makeMe.refresh(headNote2);
      List<QuizQuestion> results = controller.getAllQuizQuestionByNoteBook(headNote1);
      assertThat(results, equalTo(quizQuestionList));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsNotQuestion() {
      List<QuizQuestion> results = controller.getAllQuizQuestionByNote(noteWithoutQuestions);
      assertThat(results, hasSize(0));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsOneQuestion() {
      QuizQuestion questionOfNote =
          makeMe.aQuestion().spellingQuestionOfNote(noteWithoutQuestions).please();
      makeMe.refresh(noteWithoutQuestions);
      List<QuizQuestion> results = controller.getAllQuizQuestionByNote(noteWithoutQuestions);
      assertThat(results, contains(questionOfNote));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion() {
      QuizQuestion questionOfNote =
          makeMe.aQuestion().spellingQuestionOfNote(noteWithQuestions).please();
      List<QuizQuestion> results = controller.getAllQuizQuestionByNote(noteWithQuestions);
      assertThat(results, hasSize(2));
    }

    @Test
    void getPendingQuestionsOfANotebookWhenThereIsNotQuestion()
        throws UnexpectedNoAccessRightException {
      List<QuizQuestion> results =
          controller.getAllPendingQuizQuestionByNoteBook(noteWithoutQuestions);
      assertThat(results, hasSize(0));
    }

    @Test
    void getPendingQuestionsOfANotebookWhenThereIsOneQuestion()
        throws UnexpectedNoAccessRightException {
      QuizQuestion questionOfNote = makeMe.aQuestion().spellingQuestionOfNote(headNote1).please();
      makeMe.refresh(headNote1);
      List<QuizQuestion> results = controller.getAllPendingQuizQuestionByNoteBook(headNote1);
      assertThat(results, contains(questionOfNote));
    }
  }

  @Nested
  class approveQuestionsForAssessment {

    List<QuizQuestion> quizQuestionList = new ArrayList<>();
    Note headNote1;
    Note headNote2;
    Note noteWithoutQuestions;
    Note noteWithQuestions;
    QuizQuestion oneQuizQuestion;

    @BeforeEach
    void setUp() {
      headNote1 = makeMe.aHeadNote("headNote1").creatorAndOwner(currentUser).please();
      makeMe.theNote(headNote1).withNChildren(10).please();

      headNote2 = makeMe.aHeadNote("headNote2").creatorAndOwner(currentUser).please();
      makeMe.theNote(headNote2).withNChildren(20).please();

      makeMe.refresh(headNote1);
      makeMe.refresh(headNote2);

      noteWithoutQuestions = makeMe.aNote("a note").under(headNote1).please();
      noteWithQuestions =
          makeMe.aNote("a note with 1 questions").creatorAndOwner(currentUser).please();
      oneQuizQuestion = makeMe.aQuestion().spellingQuestionOfNote(noteWithQuestions).please();
    }

    @Test
    void shouldReviewOnePendingQuestionWhenReviewingQuestions()
        throws UnexpectedNoAccessRightException {
      List<QuizQuestion> questions = new ArrayList<>();
      questions.add(oneQuizQuestion);
      controller.approveQuizQuestion(questions);

      makeMe.refresh(oneQuizQuestion);

      assertTrue(oneQuizQuestion.isApproved());
      assertTrue(oneQuizQuestion.isReviewed());
    }

    @Test
    void shouldNotReviewQuizQuestionWhenNotOwner() {
      Note note = makeMe.aNote().please();
      QuizQuestion quizQuestion = makeMe.aQuestion().spellingQuestionOfNote(note).please();
      List<QuizQuestion> questions = new ArrayList<>();
      questions.add(quizQuestion);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.approveQuizQuestion(questions));
    }

    @Test
    void reviewedQuizQuestionsShouldBeReviewed() throws UnexpectedNoAccessRightException {
      QuizQuestion quizQuestion1 =
          makeMe.aQuestion().spellingQuestionOfNote(noteWithQuestions).please();
      quizQuestion1.setApproved(true);
      makeMe.refresh(quizQuestion1);
      QuizQuestion quizQuestion2 =
          makeMe.aQuestion().spellingQuestionOfNote(noteWithQuestions).please();

      assertFalse(quizQuestion1.isReviewed());
      assertFalse(quizQuestion2.isReviewed());
      controller.approveQuizQuestion(List.of(quizQuestion1, quizQuestion2));

      assertTrue(quizQuestion1.isReviewed());
      assertTrue(quizQuestion2.isReviewed());

      assertTrue(quizQuestion1.isApproved());
      assertFalse(quizQuestion2.isApproved());
    }
  }
}
