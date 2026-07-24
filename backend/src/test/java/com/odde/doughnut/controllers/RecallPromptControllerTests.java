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
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.NoteAliasIndexService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.odde.doughnut.utils.TimestampOperations;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import jakarta.persistence.EntityManager;
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
  @Autowired NoteAliasIndexService noteAliasIndexService;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired EntityManager entityManager;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testabilitySettings.setRandomization(new Randomization(first, 1));

    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);

    // Default structured response for question evaluation flows in this test class
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = false;
    evaluation.correctChoices = new int[] {0};
    evaluation.improvementAdvices = "This question needs improvement";
    openAiStructuredResponseMock.stubStructuredResponse(evaluation);
  }

  RecallPromptController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  MemoryTracker memoryTrackerOwnedByAnotherUser() {
    return makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
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
              .forgettingCurveAndNextRecallAt(200.0f)
              .please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      recallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .ofAIGeneratedQuestion(mcqWithAnswer, answerNote)
              .please();
      answerDTO.setChoiceIndex(0);
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
      Integer oldRecallCount = memoryTracker.getRecallCount();
      AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.getAnswer().getCorrect(), is(true));
      assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
    }

    @Test
    void shouldUpdateLinkedPropertyMemoryTrackerWhenAnsweringPropertyQuestion()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().please();
      MemoryTracker noteLevelTracker =
          makeMe
              .aMemoryTrackerFor(note)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .please();
      MemoryTracker propertyTracker =
          makeMe
              .aMemoryTrackerFor(note)
              .by(currentUser.getUser())
              .propertyKey("topic")
              .forgettingCurveAndNextRecallAt(200.0f)
              .please();
      testabilitySettings.timeTravelTo(propertyTracker.getNextRecallAt());

      Integer noteLevelRecallCountBefore = noteLevelTracker.getRecallCount();
      Float noteLevelIndexBefore = noteLevelTracker.getForgettingCurveIndex();
      Integer propertyRecallCountBefore = propertyTracker.getRecallCount();

      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt propertyRecallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(propertyTracker)
              .ofAIGeneratedQuestion(mcqWithAnswer, note)
              .please();

      controller.answerQuiz(propertyRecallPrompt, answerDTO);

      assertThat(noteLevelTracker.getRecallCount(), equalTo(noteLevelRecallCountBefore));
      assertThat(noteLevelTracker.getForgettingCurveIndex(), equalTo(noteLevelIndexBefore));
      assertThat(propertyTracker.getRecallCount(), greaterThan(propertyRecallCountBefore));
    }

    @Test
    void shouldSaveThinkingTimeMs() throws UnexpectedNoAccessRightException {
      answerDTO.setThinkingTimeMs(5000);
      AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.getAnswer().getThinkingTimeMs(), equalTo(5000));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void fastAnswer_shouldIncreaseIndexMoreThanSlowAnswer()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Fast answer (10 seconds)
      answerDTO.setThinkingTimeMs(10000);
      controller.answerQuiz(recallPrompt, answerDTO);
      Float indexWithFastAnswer = memoryTracker.getForgettingCurveIndex();

      // Reset and try slow answer (40 seconds)
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt secondRecallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .ofAIGeneratedQuestion(mcqWithAnswer, memoryTracker.getNote())
              .please();
      answerDTO.setThinkingTimeMs(40000);
      answerDTO.setChoiceIndex(0);
      controller.answerQuiz(secondRecallPrompt, answerDTO);
      Float indexWithSlowAnswer = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithFastAnswer, greaterThan(indexWithSlowAnswer));
    }

    @Test
    void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Answer with base thinking time (base case)
      answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
      controller.answerQuiz(recallPrompt, answerDTO);
      Float indexWithBaseThinkingTime = memoryTracker.getForgettingCurveIndex();

      // Reset and answer without thinking time
      memoryTracker.setForgettingCurveIndex(baseIndex);
      memoryTracker.setLastRecalledAt(baseLastRecalledAt);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt secondRecallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .ofAIGeneratedQuestion(mcqWithAnswer, memoryTracker.getNote())
              .please();
      answerDTO.setThinkingTimeMs(null);
      answerDTO.setChoiceIndex(0);
      controller.answerQuiz(secondRecallPrompt, answerDTO);
      Float indexWithoutThinkingTime = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithBaseThinkingTime, equalTo(indexWithoutThinkingTime));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(recallPrompt, answer));
    }

    @Test
    void shouldNotBeAbleToAnswerQuizForOthersMemoryTracker() {
      MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
      RecallPrompt othersPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(othersTracker)
              .withPredefinedQuestionForNote(othersTracker.getNote())
              .please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.answerQuiz(othersPrompt, answerDTO));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setChoiceIndex(1);
      }

      @Test
      void shouldValidateTheWrongAnswer() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRecallCount = memoryTracker.getRecallCount();
        AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(answerResult.getAnswer().getCorrect(), is(false));
        assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() throws UnexpectedNoAccessRightException {
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
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      recallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .withPredefinedQuestionForNote(note)
              .please();
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
    void shouldNotBeAbleToRegenerateForOthersMemoryTracker() {
      MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
      RecallPrompt othersPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(othersTracker)
              .withPredefinedQuestionForNote(othersTracker.getNote())
              .please();
      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.regenerate(othersPrompt, contestResult));
    }

    @Test
    void shouldPassOldQuestionAndContestResultToOpenAiApi()
        throws JsonProcessingException, UnexpectedNoAccessRightException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the Responses API calls
      openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      RecallQuestion regeneratedQuestion = controller.regenerate(recallPrompt, contestResult);

      Assertions.assertThat(regeneratedQuestion.getMultipleChoicesQuestion().getQuestionStem())
          .contains("What is the first color in the rainbow?");

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<MCQWithAnswer>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService(), atLeastOnce())
          .create(paramsCaptor.capture());

      String inputText =
          paramsCaptor.getValue().rawParams().input().flatMap(input -> input.text()).orElse("");
      assertThat(inputText, containsString("Previously generated non-feasible question"));
      assertThat(inputText, containsString("Improvement advice"));
      assertThat(inputText, containsString("test"));
      assertThat(
          inputText,
          containsString("Please regenerate or refine the question based on the above advice"));
    }

    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(
          OpenAiNotAvailableException.class,
          () -> controller.regenerate(recallPrompt, contestResult));
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
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      recallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .ofAIGeneratedQuestion(aiGeneratedQuestion, note)
              .please();
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
    void shouldNotBeAbleToContestForOthersMemoryTracker() {
      MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
      MCQWithAnswer aiGeneratedQuestion = makeMe.aMCQWithAnswer().please();
      RecallPrompt othersPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(othersTracker)
              .ofAIGeneratedQuestion(aiGeneratedQuestion, othersTracker.getNote())
              .please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.contest(othersPrompt));
    }

    @Test
    void rejected() throws UnexpectedNoAccessRightException {
      questionEvaluation.feasibleQuestion = true;
      openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

      QuestionContestResult contest = controller.contest(recallPrompt);
      assertTrue(contest.rejected);
      assertThat(recallPrompt.getPredefinedQuestion().isContested(), equalTo(false));
    }

    @Test
    void acceptTheContest() throws UnexpectedNoAccessRightException {
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
      questionEvaluation.feasibleQuestion = false;
      openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

      QuestionContestResult contestResult = controller.contest(recallPrompt);
      assertFalse(contestResult.rejected);
      assertThat(recallPrompt.getPredefinedQuestion().isContested(), equalTo(true));

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<QuestionEvaluation>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      assertThat(
          paramsCaptor.getValue().rawParams().model().orElseThrow().asString(), equalTo("gpt-new"));
    }

    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(OpenAiNotAvailableException.class, () -> controller.contest(recallPrompt));
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
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
      recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      answerDTO.setSpellingAnswer(answerNote.getTitle());
    }

    @Test
    void spellingQuestionMasksFrontmatterAliasesInStem() {
      makeMe
          .theNote(answerNote)
          .title("colour")
          .content(
              """
              ---
              aliases:
                - color
              ---
              The color of the sky is blue
              """)
          .please();
      RecallPrompt spellingPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

      SpellingQuestion question = spellingPrompt.getSpellingQuestion();

      assertThat(question, notNullValue());
      assertThat(question.getStem(), containsString("The "));
      assertThat(question.getStem(), containsString("<mark"));
      assertThat(question.getStem(), not(containsString("color")));
    }

    @Test
    void answerOneOfTheFrontmatterAliases() throws UnexpectedNoAccessRightException {
      makeMe
          .theNote(answerNote)
          .title("this")
          .content(
              """
              ---
              aliases:
                - that
              ---
              Body text
              """)
          .please();
      answerDTO.setSpellingAnswer("this");
      assertTrue(controller.answerSpelling(recallPrompt, answerDTO).getAnswer().getCorrect());
      RecallPrompt secondRecallPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      AnswerSpellingDTO secondAnswerDTO = new AnswerSpellingDTO();
      secondAnswerDTO.setSpellingAnswer("that");
      assertTrue(
          controller.answerSpelling(secondRecallPrompt, secondAnswerDTO).getAnswer().getCorrect());
    }

    @Test
    void spellingQuestionMasksPlainAliasButNotOverlapWikiLinkTargetTitle() {
      makeMe
          .theNote(answerNote)
          .title("colour")
          .content(
              """
              ---
              aliases:
                - color
                - "[[Other Note]]"
              ---
              The color of Other Note is blue
              """)
          .please();
      RecallPrompt spellingPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

      SpellingQuestion question = spellingPrompt.getSpellingQuestion();

      assertThat(question, notNullValue());
      assertThat(question.getStem(), containsString("<mark"));
      assertThat(question.getStem(), not(containsString("color")));
      assertThat(question.getStem(), containsString("Other Note"));
    }

    @Test
    void spellingQuestionDoesNotMaskOverlapTargetTitleFromWikiLinkOnlyAlias() {
      makeMe
          .theNote(answerNote)
          .title("colour")
          .content(
              """
              ---
              aliases:
                - "[[Other Note]]"
              ---
              Mentions Other Note in the body
              """)
          .please();
      RecallPrompt spellingPrompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

      SpellingQuestion question = spellingPrompt.getSpellingQuestion();

      assertThat(question, notNullValue());
      assertThat(question.getStem(), containsString("Other Note"));
      assertThat(question.getStem(), not(containsString("<mark")));
    }

    @Test
    void answerDoesNotMatchOverlapWikiLinkAliasTargetOrRawToken()
        throws UnexpectedNoAccessRightException {
      makeMe
          .theNote(answerNote)
          .title("this")
          .content(
              """
              ---
              aliases:
                - that
                - "[[Other Note]]"
              ---
              Body text
              """)
          .please();

      AnswerSpellingDTO plainAliasAnswer = new AnswerSpellingDTO();
      plainAliasAnswer.setSpellingAnswer("that");
      assertTrue(
          controller
              .answerSpelling(
                  makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(),
                  plainAliasAnswer)
              .getAnswer()
              .getCorrect());

      AnswerSpellingDTO overlapTitleAnswer = new AnswerSpellingDTO();
      overlapTitleAnswer.setSpellingAnswer("Other Note");
      AnsweredQuestion overlapTitleResult =
          controller.answerSpelling(
              makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(),
              overlapTitleAnswer);
      assertFalse(overlapTitleResult.getAnswer().getCorrect());
      assertNull(overlapTitleResult.getOverlap());

      AnswerSpellingDTO rawTokenAnswer = new AnswerSpellingDTO();
      rawTokenAnswer.setSpellingAnswer("[[Other Note]]");
      AnsweredQuestion rawTokenResult =
          controller.answerSpelling(
              makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(),
              rawTokenAnswer);
      assertFalse(rawTokenResult.getAnswer().getCorrect());
      assertNull(rawTokenResult.getOverlap());
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
      Integer oldRecallCount = memoryTracker.getRecallCount();
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.getAnswer().getCorrect());
      assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
    }

    @Test
    void shouldAcceptThinkingTimeMs() throws UnexpectedNoAccessRightException {
      answerDTO.setThinkingTimeMs(5000);
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.getAnswer().getCorrect());
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
      Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void fastAnswer_shouldIncreaseIndexMoreThanSlowAnswer()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Fast answer (10 seconds)
      answerDTO.setThinkingTimeMs(10000);
      controller.answerSpelling(recallPrompt, answerDTO);
      Float indexWithFastAnswer = memoryTracker.getForgettingCurveIndex();

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
      Float indexWithSlowAnswer = memoryTracker.getForgettingCurveIndex();

      assertThat(indexWithFastAnswer, greaterThan(indexWithSlowAnswer));
    }

    @Test
    void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float baseIndex = memoryTracker.getForgettingCurveIndex();
      Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

      // Answer with base thinking time (base case)
      answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
      controller.answerSpelling(recallPrompt, answerDTO);
      Float indexWithBaseThinkingTime = memoryTracker.getForgettingCurveIndex();

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
      Float indexWithoutThinkingTime = memoryTracker.getForgettingCurveIndex();

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

    @Test
    void shouldNotPopulateAccidentalMatchFieldsOnCorrectSpellingAnswer()
        throws UnexpectedNoAccessRightException {
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertTrue(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getOverlap());
      assertNull(answerResult.getMatchedNotes());
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
        Integer oldRecallCount = memoryTracker.getRecallCount();
        AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
        assertFalse(answerResult.getAnswer().getCorrect());
        assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() throws UnexpectedNoAccessRightException {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
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

      @Test
      void shouldNotPopulateAccidentalMatchFieldsOnWrongSpellingAnswer()
          throws UnexpectedNoAccessRightException {
        AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
        assertFalse(answerResult.getAnswer().getCorrect());
        assertNull(answerResult.getAnswer().getMatchedNoteId());
        assertNull(answerResult.getAnswer().getOutcome());
        assertNull(answerResult.getOverlap());
        assertNull(answerResult.getMatchedNotes());
      }
    }
  }

  @Nested
  class AccidentalMatch {
    Note answerNote;
    Note secondNote;
    MemoryTracker memoryTracker;
    RecallPrompt recallPrompt;
    AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

    @BeforeEach
    void setup() {
      answerNote = makeMe.aNote().rememberSpelling().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
      recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      secondNote = makeMe.aNote().notebook(otherNotebook).title("Another Note Title").please();
      answerDTO.setSpellingAnswer(secondNote.getTitle());
    }

    @Test
    void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteTitle()
        throws UnexpectedNoAccessRightException {
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(), equalTo(secondNote.getId().longValue()));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(answerResult.getMatchedNotes(), hasSize(1));
      assertThat(answerResult.getMatchedNotes().getFirst().getId(), equalTo(secondNote.getId()));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo((long) answerResult.getMatchedNotes().getFirst().getId()));
      assertThat(
          answerResult.getMatchedNotes().getFirst().getTitle(), equalTo(secondNote.getTitle()));
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldPreferLowestNoteIdWhenMultipleReadableNotesShareTitle()
        throws UnexpectedNoAccessRightException {
      Notebook thirdNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note thirdNote = makeMe.aNote().notebook(thirdNotebook).title(secondNote.getTitle()).please();
      assertThat(secondNote.getId(), lessThan(thirdNote.getId()));

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(), equalTo(secondNote.getId().longValue()));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(
          answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
          contains(secondNote.getId(), thirdNote.getId()));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo((long) answerResult.getMatchedNotes().getFirst().getId()));
    }

    @Test
    void shouldIncludeTitleAndAliasMatchesInMatchedNotesOrderedById()
        throws UnexpectedNoAccessRightException {
      String shared = "TitlePreferredMatch";
      Notebook notebookA = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note noteA = makeMe.aNote().notebook(notebookA).title(shared).please();
      Notebook notebookB = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note noteB =
          makeMe
              .aNote()
              .notebook(notebookB)
              .title("AliasOnlyNoteTitle")
              .content("---\naliases:\n  - " + shared + "\n---\n\nbody")
              .please();
      noteAliasIndexService.refreshForNote(noteB);
      answerDTO.setSpellingAnswer(shared);

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      int minId = Math.min(noteA.getId(), noteB.getId());
      int maxId = Math.max(noteA.getId(), noteB.getId());
      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(
          answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
          contains(minId, maxId));
      assertThat(answerResult.getAnswer().getMatchedNoteId(), equalTo((long) minId));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo((long) answerResult.getMatchedNotes().getFirst().getId()));
    }

    @Test
    void shouldGradeBlankAnswerAsPlainWrongEvenWhenEmptyTitleReadableNoteExists()
        throws UnexpectedNoAccessRightException {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note emptyTitleNote = makeMe.aNote().notebook(otherNotebook).title("temp").please();
      entityManager
          .createQuery("UPDATE Note n SET n.title = '' WHERE n.id = :id")
          .setParameter("id", emptyTitleNote.getId())
          .executeUpdate();
      entityManager.flush();
      answerDTO.setSpellingAnswer("");

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
      assertNull(answerResult.getMatchedNotes());
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldGradeWhitespaceOnlyAnswerAsPlainWrong() throws UnexpectedNoAccessRightException {
      answerDTO.setSpellingAnswer("   \t  ");

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
    }

    @Test
    void shouldApplyLighterPenaltyThanWrongAnswer() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(190.0f));
      assertThat(
          memoryTracker.getNextRecallAt(),
          greaterThan(testabilitySettings.getCurrentUTCTimestamp()));
      assertThat(
          memoryTracker.getNextRecallAt(),
          not(
              equalTo(
                  TimestampOperations.addHoursToTimestamp(
                      testabilitySettings.getCurrentUTCTimestamp(), 12))));
    }

    @Test
    void shouldNotLeakMatchedNoteIdFromUnreadableNotebook()
        throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Notebook unreadableNotebook = makeMe.aNotebook().creatorAndOwner(otherUser).please();
      Note unreadableNote =
          makeMe.aNote().notebook(unreadableNotebook).title("Unreadable Accidental Title").please();
      answerDTO.setSpellingAnswer(unreadableNote.getTitle());

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
      assertNull(answerResult.getMatchedNotes());
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldOmitUnreadableNotesFromMatchedNotesWhenReadableMatchAlsoExists()
        throws UnexpectedNoAccessRightException {
      String sharedTitle = "Shared Accidental Title";
      User otherUser = makeMe.aUser().please();
      Notebook unreadableNotebook = makeMe.aNotebook().creatorAndOwner(otherUser).please();
      Note unreadableNote = makeMe.aNote().notebook(unreadableNotebook).title(sharedTitle).please();
      Notebook readableNotebook =
          makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note readableNote = makeMe.aNote().notebook(readableNotebook).title(sharedTitle).please();
      answerDTO.setSpellingAnswer(sharedTitle);

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(answerResult.getMatchedNotes(), hasSize(1));
      assertThat(answerResult.getMatchedNotes().getFirst().getId(), equalTo(readableNote.getId()));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(), equalTo(readableNote.getId().longValue()));
      assertThat(
          answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
          not(hasItem(unreadableNote.getId())));
    }

    @Test
    void shouldSkipAccidentalMatchSearchWhenAnswerMatchesReviewedNoteEvenIfAnotherNoteSharesTitle()
        throws UnexpectedNoAccessRightException {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aNote().notebook(otherNotebook).title(answerNote.getTitle()).please();
      answerDTO.setSpellingAnswer(answerNote.getTitle());

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertTrue(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
      assertNull(answerResult.getMatchedNotes());
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteAlias()
        throws UnexpectedNoAccessRightException {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      String alias = "AccidentalAliasMatch";
      Note aliasBearingNote =
          makeMe
              .aNote()
              .notebook(otherNotebook)
              .title("Unrelated Note Title")
              .content("---\naliases:\n  - " + alias + "\n---\n\nbody")
              .please();
      noteAliasIndexService.refreshForNote(aliasBearingNote);
      answerDTO.setSpellingAnswer(alias);

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo(aliasBearingNote.getId().longValue()));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(answerResult.getMatchedNotes(), hasSize(1));
      assertThat(
          answerResult.getMatchedNotes().getFirst().getId(), equalTo(aliasBearingNote.getId()));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo((long) answerResult.getMatchedNotes().getFirst().getId()));
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldNotAccidentalMatchViaWikiLinkOverlapAliasItem()
        throws UnexpectedNoAccessRightException {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      String overlapTargetTitle = "OverlapTargetTitle";
      Note overlapDeclarer =
          makeMe
              .aNote()
              .notebook(otherNotebook)
              .title("Unrelated Overlap Declarer")
              .content(
                  """
                  ---
                  aliases:
                    - "[[OverlapTargetTitle]]"
                  ---

                  body
                  """)
              .please();
      noteAliasIndexService.refreshForNote(overlapDeclarer);
      answerDTO.setSpellingAnswer(overlapTargetTitle);

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertNull(answerResult.getAnswer().getMatchedNoteId());
      assertNull(answerResult.getMatchedNotes());
      assertNull(answerResult.getOverlap());
    }

    @Test
    void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAliasAfterTrim()
        throws UnexpectedNoAccessRightException {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      String alias = "AccidentalAliasMatch";
      Note aliasBearingNote =
          makeMe
              .aNote()
              .notebook(otherNotebook)
              .title("Unrelated Note Title For Trim")
              .content("---\naliases:\n  - " + alias + "\n---\n\nbody")
              .please();
      noteAliasIndexService.refreshForNote(aliasBearingNote);
      answerDTO.setSpellingAnswer("  " + alias + "  ");

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertFalse(answerResult.getAnswer().getCorrect());
      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo(aliasBearingNote.getId().longValue()));
      assertThat(answerResult.getMatchedNotes(), notNullValue());
      assertThat(answerResult.getMatchedNotes(), hasSize(1));
      assertThat(
          answerResult.getMatchedNotes().getFirst().getId(), equalTo(aliasBearingNote.getId()));
      assertThat(
          answerResult.getAnswer().getMatchedNoteId(),
          equalTo((long) answerResult.getMatchedNotes().getFirst().getId()));
    }

    @Test
    void shouldNotDropForgettingCurveIndexBelowFloorOnAccidentalMatch()
        throws UnexpectedNoAccessRightException {
      memoryTracker.setForgettingCurveIndex(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX);
      memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
      makeMe.entityPersister.save(memoryTracker);
      answerDTO.setSpellingAnswer(secondNote.getTitle());
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());

      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

      assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertThat(
          memoryTracker.getForgettingCurveIndex(),
          equalTo(ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX));
      assertThat(
          memoryTracker.getNextRecallAt(),
          greaterThanOrEqualTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldStillCountAccidentalMatchTowardWrongAnswerThreshold()
        throws UnexpectedNoAccessRightException {
      answerDTO.setSpellingAnswer(secondNote.getTitle());
      Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
      assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

      for (int i = 0; i < 4; i++) {
        RecallPrompt prompt =
            makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
        AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);
        assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
        assertFalse(result.getAnswer().getCorrect());
      }
      assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

      AnsweredQuestion fifth =
          controller.answerSpelling(
              makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please(),
              answerDTO);
      assertThat(fifth.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
      assertFalse(fifth.getAnswer().getCorrect());
      assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(true));
    }
  }

  @Nested
  class OverlapTryAgain {
    Note reviewedNote;
    Note partnerNote;
    MemoryTracker memoryTracker;

    @BeforeEach
    void setup() {
      Notebook partnerNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      partnerNote = makeMe.aNote().notebook(partnerNotebook).title("Shared Title").please();
      String overlapToken = partnerNotebook.getName() + ":Shared Title";
      reviewedNote =
          makeMe
              .aNote()
              .rememberSpelling()
              .title("Shared Title")
              .content(
                  """
                  ---
                  aliases:
                    - color
                    - "[[%s]]"
                  ---
                  Body text
                  """
                      .formatted(overlapToken))
              .please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(reviewedNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
    }

    @Test
    void shouldGradeAsOverlapWhenAnswerMatchesReviewedAndResolvedOverlapTarget()
        throws UnexpectedNoAccessRightException {
      Integer recallCountBefore = memoryTracker.getRecallCount();
      Float forgettingCurveBefore = memoryTracker.getForgettingCurveIndex();
      Timestamp nextRecallAtBefore = memoryTracker.getNextRecallAt();

      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("Shared Title");
      RecallPrompt prompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

      AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);

      assertFalse(result.getAnswer().getCorrect());
      assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.OVERLAP));
      assertThat(result.getOverlap(), is(true));
      assertThat(result.getMatchedNotes(), anyOf(nullValue(), empty()));
      assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore));
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(forgettingCurveBefore));
      assertThat(memoryTracker.getNextRecallAt(), equalTo(nextRecallAtBefore));
    }

    @Test
    void shouldGradeCorrectWithCreditWhenDistinguishingPlainAlias()
        throws UnexpectedNoAccessRightException {
      Integer recallCountBefore = memoryTracker.getRecallCount();

      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("color");
      RecallPrompt prompt =
          makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

      AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);

      assertTrue(result.getAnswer().getCorrect());
      assertThat(result.getAnswer().getOutcome(), not(is(AnswerOutcome.OVERLAP)));
      assertThat(result.getOverlap(), anyOf(nullValue(), is(false)));
      assertThat(memoryTracker.getRecallCount(), equalTo(recallCountBefore + 1));
    }

    @Test
    void shouldNotCountOverlapTowardWrongAnswerThreshold() throws UnexpectedNoAccessRightException {
      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("Shared Title");
      Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
      assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));

      for (int i = 0; i < 5; i++) {
        RecallPrompt prompt =
            makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
        AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);
        assertThat(result.getAnswer().getOutcome(), is(AnswerOutcome.OVERLAP));
        assertFalse(result.getAnswer().getCorrect());
      }
      assertThat(memoryTrackerService.isThresholdExceeded(memoryTracker, now), is(false));
    }

    @Test
    void shouldGradeCorrectWithCreditWhenOverlapTargetDoesNotExist()
        throws UnexpectedNoAccessRightException {
      Note noteWithDeadOverlap =
          makeMe
              .aNote()
              .rememberSpelling()
              .title("Reviewed Alone")
              .content(
                  """
                  ---
                  aliases:
                    - "[[No Such Notebook:Missing Partner Title]]"
                  ---
                  Body text
                  """)
              .please();
      MemoryTracker tracker =
          makeMe
              .aMemoryTrackerFor(noteWithDeadOverlap)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
      Integer recallCountBefore = tracker.getRecallCount();

      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("Reviewed Alone");
      RecallPrompt prompt = makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please();

      AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);

      assertTrue(result.getAnswer().getCorrect());
      assertThat(result.getAnswer().getOutcome(), not(is(AnswerOutcome.OVERLAP)));
      assertThat(result.getOverlap(), anyOf(nullValue(), is(false)));
      assertThat(tracker.getRecallCount(), equalTo(recallCountBefore + 1));
    }

    @Test
    void shouldGradeCorrectWithCreditWhenOverlapPartnerIsUnreadable()
        throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Notebook unreadableNotebook = makeMe.aNotebook().creatorAndOwner(otherUser).please();
      makeMe.aNote().notebook(unreadableNotebook).title("Shared Unreadable").please();
      String overlapToken = unreadableNotebook.getName() + ":Shared Unreadable";
      Note reviewed =
          makeMe
              .aNote()
              .rememberSpelling()
              .title("Shared Unreadable")
              .content(
                  """
                  ---
                  aliases:
                    - "[[%s]]"
                  ---
                  Body text
                  """
                      .formatted(overlapToken))
              .please();
      MemoryTracker tracker =
          makeMe
              .aMemoryTrackerFor(reviewed)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
      Integer recallCountBefore = tracker.getRecallCount();

      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("Shared Unreadable");
      RecallPrompt prompt = makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please();

      AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);

      assertTrue(result.getAnswer().getCorrect());
      assertThat(result.getAnswer().getOutcome(), not(is(AnswerOutcome.OVERLAP)));
      assertThat(result.getOverlap(), anyOf(nullValue(), is(false)));
      assertThat(tracker.getRecallCount(), equalTo(recallCountBefore + 1));
    }

    @Test
    void shouldGradeCorrectWithCreditWhenOverlapTokenIsSelfReferential()
        throws UnexpectedNoAccessRightException {
      Note selfOverlapping =
          makeMe
              .aNote()
              .rememberSpelling()
              .title("Self Referential Title")
              .content(
                  """
                  ---
                  aliases:
                    - "[[Self Referential Title]]"
                  ---
                  Body text
                  """)
              .please();
      MemoryTracker tracker =
          makeMe
              .aMemoryTrackerFor(selfOverlapping)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200.0f)
              .spelling()
              .please();
      Integer recallCountBefore = tracker.getRecallCount();

      AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();
      answerDTO.setSpellingAnswer("Self Referential Title");
      RecallPrompt prompt = makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please();

      AnsweredQuestion result = controller.answerSpelling(prompt, answerDTO);

      assertTrue(result.getAnswer().getCorrect());
      assertThat(result.getAnswer().getOutcome(), not(is(AnswerOutcome.OVERLAP)));
      assertThat(result.getOverlap(), anyOf(nullValue(), is(false)));
      assertThat(tracker.getRecallCount(), equalTo(recallCountBefore + 1));
    }
  }
}
