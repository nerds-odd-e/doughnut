package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.first;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.utils.TimestampOperations;
import com.openai.client.OpenAIClient;
import java.sql.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptControllerTests extends ControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired RecallPromptController controller;
  @Autowired GlobalSettingsService globalSettingsService;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testabilitySettings.setRandomization(new Randomization(first, 1));

    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Mock chat completion for question evaluation
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = false;
    evaluation.correctChoices = new int[] {0};
    evaluation.improvementAdvices = "This question needs improvement";
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(evaluation);
  }

  RecallPromptController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  @Nested
  class answerQuizQuestion {
    MemoryTracker memoryTracker;
    RecallPrompt recallPrompt;
    AnswerDTO answerDTO = new AnswerDTO();

    @BeforeEach
    void setup() {
      Note answerNote = makeMe.aNote().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200)
              .please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      recallPrompt =
          makeMe.aRecallPrompt().ofAIGeneratedQuestion(mcqWithAnswer, answerNote).please();
      answerDTO.setChoiceIndex(0);
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() {
      Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
      RecallResult.QuestionResult answerResult =
          (RecallResult.QuestionResult) controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.answeredQuestion().answer.getCorrect(), is(true));
      assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldSaveThinkingTimeMs() {
      answerDTO.setThinkingTimeMs(5000);
      RecallResult.QuestionResult answerResult =
          (RecallResult.QuestionResult) controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.answeredQuestion().answer.getThinkingTimeMs(), equalTo(5000));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void fastAnswer_shouldIncreaseIndexMoreThanSlowAnswer() {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Fast answer (10 seconds)
      answerDTO.setThinkingTimeMs(10000);
      controller.answerQuiz(recallPrompt, answerDTO);
      Integer indexWithFastAnswer = memoryTracker.getForgettingCurveIndex();

      // Reset and try slow answer (40 seconds)
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt secondRecallPrompt =
          makeMe
              .aRecallPrompt()
              .ofAIGeneratedQuestion(mcqWithAnswer, memoryTracker.getNote())
              .please();
      answerDTO.setThinkingTimeMs(40000);
      answerDTO.setChoiceIndex(0);
      controller.answerQuiz(secondRecallPrompt, answerDTO);
      Integer indexWithSlowAnswer = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithFastAnswer, greaterThan(indexWithSlowAnswer));
    }

    @Test
    void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment() {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Answer with base thinking time (base case)
      answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
      controller.answerQuiz(recallPrompt, answerDTO);
      Integer indexWithBaseThinkingTime = memoryTracker.getForgettingCurveIndex();

      // Reset and answer without thinking time
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt secondRecallPrompt =
          makeMe
              .aRecallPrompt()
              .ofAIGeneratedQuestion(mcqWithAnswer, memoryTracker.getNote())
              .please();
      answerDTO.setThinkingTimeMs(null);
      answerDTO.setChoiceIndex(0);
      controller.answerQuiz(secondRecallPrompt, answerDTO);
      Integer indexWithoutThinkingTime = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithBaseThinkingTime, equalTo(indexWithoutThinkingTime));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(recallPrompt, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setChoiceIndex(1);
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
        RecallResult.QuestionResult answerResult =
            (RecallResult.QuestionResult) controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(answerResult.answeredQuestion().answer.getCorrect(), is(false));
        assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(
            memoryTracker.getNextRecallAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class RegenerateQuestion {
    RecallPrompt recallPrompt;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();
      recallPrompt = makeMe.aRecallPrompt().approvedQuestionOf(note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            currentUser.setUser(null);
            QuestionContestResult contestResult = new QuestionContestResult();
            contestResult.advice = "test";
            controller.regenerate(recallPrompt, contestResult);
          });
    }

    @Test
    void createQuizQuestion() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the chat completion API calls
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      RecallPrompt regeneratedQuestion = controller.regenerate(recallPrompt, contestResult);

      Assertions.assertThat(regeneratedQuestion.getMultipleChoicesQuestion().getF0__stem())
          .contains("What is the first color in the rainbow?");
    }

    @Test
    void shouldPassOldQuestionAndContestResultToOpenAiApi() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the chat completion API calls
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      controller.regenerate(recallPrompt, contestResult);

      // Verify chat completion call contains message with question info and contest result
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService(), atLeastOnce())
          .create(paramsCaptor.capture());

      // Check if any message contains the required contest info
      boolean hasContestInfo =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(
                  content ->
                      content.contains("Previously generated non-feasible question")
                          && content.contains("Improvement advice")
                          && content.contains("test")
                          && content.contains(
                              "Please regenerate or refine the question based on the above advice"));

      assertThat("A message should contain the contest information", hasContestInfo, is(true));
    }
  }

  @Nested
  class Contest {
    RecallPrompt recallPrompt;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.improvementAdvices = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion = makeMe.aMCQWithAnswer().please();
      Note note = makeMe.aNote().please();
      recallPrompt =
          makeMe.aRecallPrompt().ofAIGeneratedQuestion(aiGeneratedQuestion, note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            currentUser.setUser(null);
            controller.contest(recallPrompt);
          });
    }

    @Test
    void rejected() {
      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      QuestionContestResult contest = controller.contest(recallPrompt);
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");

      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      controller.contest(recallPrompt);

      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      assertThat(paramsCaptor.getValue().model().asString(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      QuestionContestResult contestResult = controller.contest(recallPrompt);
      assertFalse(contestResult.rejected);
    }
  }

  @Nested
  class AnswerSpelling {
    Note answerNote;
    MemoryTracker memoryTracker;
    RecallPrompt recallPrompt;
    AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

    @BeforeEach
    void setup() throws UnexpectedNoAccessRightException {
      answerNote = makeMe.aNote().rememberSpelling().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200)
              .spelling()
              .please();
      recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      answerDTO.setSpellingAnswer(answerNote.getTitle());
    }

    @Test
    void answerOneOfTheTitles() throws UnexpectedNoAccessRightException {
      makeMe.theNote(answerNote).title("this / that").please();
      answerDTO.setSpellingAnswer("this");
      assertTrue(
          ((RecallResult.SpellingResult) controller.answerSpelling(recallPrompt, answerDTO))
              .isCorrect());
      // Create a new recall prompt for the second answer
      RecallPrompt secondRecallPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      AnswerSpellingDTO secondAnswerDTO = new AnswerSpellingDTO();
      secondAnswerDTO.setSpellingAnswer("that");
      assertTrue(
          ((RecallResult.SpellingResult)
                  controller.answerSpelling(secondRecallPrompt, secondAnswerDTO))
              .isCorrect());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
      Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
      RecallResult.SpellingResult answerResult =
          (RecallResult.SpellingResult) controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.isCorrect());
      assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldAcceptThinkingTimeMs() throws UnexpectedNoAccessRightException {
      answerDTO.setThinkingTimeMs(5000);
      RecallResult.SpellingResult answerResult =
          (RecallResult.SpellingResult) controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.isCorrect());
      RecallPrompt reloadedPrompt = makeMe.refresh(recallPrompt);
      Answer answer = reloadedPrompt.getAnswer();
      assertNotNull(answer);
      assertThat(answer.getThinkingTimeMs(), equalTo(5000));
      assertThat(answer.getSpellingAnswer(), equalTo(answerDTO.getSpellingAnswer()));
      assertTrue(answer.getCorrect());
    }

    @Test
    void shouldCreateAnswerEntityForSpellingQuestion() throws UnexpectedNoAccessRightException {
      answerDTO.setSpellingAnswer(answerNote.getTitle());
      answerDTO.setThinkingTimeMs(3000);
      controller.answerSpelling(recallPrompt, answerDTO);

      RecallPrompt reloadedPrompt = makeMe.refresh(recallPrompt);
      Answer answer = reloadedPrompt.getAnswer();
      assertNotNull(answer);
      assertThat(answer.getSpellingAnswer(), equalTo(answerNote.getTitle()));
      assertThat(answer.getThinkingTimeMs(), equalTo(3000));
      assertTrue(answer.getCorrect());
    }

    @Test
    void shouldNotAllowAnsweringTwice() throws UnexpectedNoAccessRightException {
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThrows(
          IllegalArgumentException.class, () -> controller.answerSpelling(recallPrompt, answerDTO));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void fastAnswer_shouldIncreaseIndexMoreThanSlowAnswer()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Fast answer (10 seconds)
      answerDTO.setThinkingTimeMs(10000);
      controller.answerSpelling(recallPrompt, answerDTO);
      Integer indexWithFastAnswer = memoryTracker.getForgettingCurveIndex();

      // Reset and try slow answer (40 seconds)
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      RecallPrompt secondRecallPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      AnswerSpellingDTO secondAnswerDTO = new AnswerSpellingDTO();
      secondAnswerDTO.setSpellingAnswer(answerNote.getTitle());
      secondAnswerDTO.setThinkingTimeMs(40000);
      controller.answerSpelling(secondRecallPrompt, secondAnswerDTO);
      Integer indexWithSlowAnswer = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithFastAnswer, greaterThan(indexWithSlowAnswer));
    }

    @Test
    void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Answer with base thinking time (base case)
      answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
      controller.answerSpelling(recallPrompt, answerDTO);
      Integer indexWithBaseThinkingTime = memoryTracker.getForgettingCurveIndex();

      // Reset and answer without thinking time
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      RecallPrompt secondRecallPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      AnswerSpellingDTO secondAnswerDTO = new AnswerSpellingDTO();
      secondAnswerDTO.setSpellingAnswer(answerNote.getTitle());
      secondAnswerDTO.setThinkingTimeMs(null);
      controller.answerSpelling(secondRecallPrompt, secondAnswerDTO);
      Integer indexWithoutThinkingTime = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithBaseThinkingTime, equalTo(indexWithoutThinkingTime));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerSpellingDTO answer = new AnswerSpellingDTO();
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.answerSpelling(recallPrompt, answer));
    }

    @Test
    void shouldValidateRecallPromptIsSpellingType() {
      RecallPrompt mcqPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
      AnswerSpellingDTO answer = new AnswerSpellingDTO();
      answer.setSpellingAnswer(answerNote.getTitle());
      assertThrows(
          IllegalArgumentException.class, () -> controller.answerSpelling(mcqPrompt, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setSpellingAnswer("wrong");
      }

      @Test
      void shouldValidateTheWrongAnswer() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
        RecallResult.SpellingResult answerResult =
            (RecallResult.SpellingResult) controller.answerSpelling(recallPrompt, answerDTO);
        assertFalse(answerResult.isCorrect());
        assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerSpelling(recallPrompt, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() throws UnexpectedNoAccessRightException {
        controller.answerSpelling(recallPrompt, answerDTO);
        assertThat(
            memoryTracker.getNextRecallAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class ContestQuestion {
    RecallPrompt recallPrompt;
    Note note;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();
      recallPrompt = makeMe.aRecallPrompt().approvedQuestionOf(note).please();
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = false;
      questionEvaluation.improvementAdvices = "This is a valid contest";
    }

    @Test
    void shouldMarkQuestionAsContestedWhenContestIsAccepted() {
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      // When
      QuestionContestResult result = controller.contest(recallPrompt);

      // Then
      assertThat(result.rejected, equalTo(false));
      assertThat(recallPrompt.getPredefinedQuestion().isContested(), equalTo(true));
    }

    @Test
    void shouldNotMarkQuestionAsContestedWhenContestIsRejected() {
      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      // When
      QuestionContestResult result = controller.contest(recallPrompt);

      // Then
      assertThat(result.rejected, equalTo(true));
      assertThat(recallPrompt.getPredefinedQuestion().isContested(), equalTo(false));
    }
  }
}
