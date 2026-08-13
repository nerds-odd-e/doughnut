package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptAnswerSpellingControllerTest extends RecallPromptControllerTestBase {
  Note answerNote;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO;

  @BeforeEach
  void setup() {
    answerNote = ownedSpellingNote();
    memoryTracker = ownedSpellingTracker(answerNote);
    recallPrompt = spellingPrompt(memoryTracker);
    answerDTO = spellingAnswer(answerNote.getTitle());
  }

  @Test
  void answerOneOfTheFrontmatterAliases() throws UnexpectedNoAccessRightException {
    makeMe.theNote(answerNote).title("this").aliases("that").please();

    assertTrue(
        controller.answerSpelling(recallPrompt, spellingAnswer("this")).getAnswer().getCorrect());
    assertTrue(
        controller
            .answerSpelling(spellingPrompt(memoryTracker), spellingAnswer("that"))
            .getAnswer()
            .getCorrect());
  }

  @Test
  void answerDoesNotMatchOverlapWikiLinkAliasTargetOrRawToken()
      throws UnexpectedNoAccessRightException {
    makeMe.theNote(answerNote).title("this").aliases("that").overlapWikiLink("Other Note").please();

    AnsweredQuestion overlapTitleResult =
        controller.answerSpelling(spellingPrompt(memoryTracker), spellingAnswer("Other Note"));
    assertFalse(overlapTitleResult.getAnswer().getCorrect());

    AnsweredQuestion rawTokenResult =
        controller.answerSpelling(spellingPrompt(memoryTracker), spellingAnswer("[[Other Note]]"));
    assertFalse(rawTokenResult.getAnswer().getCorrect());
  }

  @Test
  void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
    Integer oldRecallCount = memoryTracker.getRecallCount();
    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
    assertTrue(answerResult.getAnswer().getCorrect());
    assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
  }

  @Test
  void shouldPersistThinkingTimeAndSpellingOnAnswerEntity()
      throws UnexpectedNoAccessRightException {
    answerDTO.setThinkingTimeMs(5000);
    controller.answerSpelling(recallPrompt, answerDTO);

    Answer answer = makeMe.refresh(recallPrompt).getAnswer();
    assertThat(answer.getThinkingTimeMs(), equalTo(5000));
    assertThat(answer.getSpellingAnswer(), equalTo(answerDTO.getSpellingAnswer()));
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
  void lateCorrectAnswerDoesNotShortenTheNextInterval() throws UnexpectedNoAccessRightException {
    Note lateNote = ownedSpellingNote();
    MemoryTracker lateTracker = ownedSpellingTracker(lateNote);
    Integer thinkingTimeMs = ForgettingCurve.BASE_THINKING_TIME_MS;

    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    AnswerSpellingDTO onTimeAnswer = spellingAnswer(answerNote.getTitle());
    onTimeAnswer.setThinkingTimeMs(thinkingTimeMs);
    controller.answerSpelling(recallPrompt, onTimeAnswer);
    long onTimeIntervalMs =
        memoryTracker.getNextRecallAt().getTime()
            - testabilitySettings.getCurrentUTCTimestamp().getTime();

    Timestamp lateAnswerAt =
        TimestampOperations.addHoursToTimestamp(lateTracker.getNextRecallAt(), 100 * 24);
    testabilitySettings.timeTravelTo(lateAnswerAt);
    AnswerSpellingDTO lateAnswer = spellingAnswer(lateNote.getTitle());
    lateAnswer.setThinkingTimeMs(thinkingTimeMs);
    controller.answerSpelling(spellingPrompt(lateTracker), lateAnswer);
    long lateIntervalMs =
        lateTracker.getNextRecallAt().getTime()
            - testabilitySettings.getCurrentUTCTimestamp().getTime();

    assertThat(lateIntervalMs, greaterThanOrEqualTo(onTimeIntervalMs));
  }

  @Test
  void fastAnswer_shouldIncreaseIndexMoreThanSlowAnswer() throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    Float baseIndex = memoryTracker.getForgettingCurveIndex();
    Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

    answerDTO.setThinkingTimeMs(10000);
    controller.answerSpelling(recallPrompt, answerDTO);
    Float indexWithFastAnswer = memoryTracker.getForgettingCurveIndex();

    memoryTracker.setForgettingCurveIndex(baseIndex);
    memoryTracker.setLastRecalledAt(baseLastRecalledAt);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    AnswerSpellingDTO slowAnswer = spellingAnswer(answerNote.getTitle());
    slowAnswer.setThinkingTimeMs(40000);
    controller.answerSpelling(spellingPrompt(memoryTracker), slowAnswer);

    assertThat(indexWithFastAnswer, greaterThan(memoryTracker.getForgettingCurveIndex()));
  }

  @Test
  void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    Float baseIndex = memoryTracker.getForgettingCurveIndex();
    Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

    answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
    controller.answerSpelling(recallPrompt, answerDTO);
    Float indexWithBaseThinkingTime = memoryTracker.getForgettingCurveIndex();

    memoryTracker.setForgettingCurveIndex(baseIndex);
    memoryTracker.setLastRecalledAt(baseLastRecalledAt);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    AnswerSpellingDTO noThinkingTime = spellingAnswer(answerNote.getTitle());
    noThinkingTime.setThinkingTimeMs(null);
    controller.answerSpelling(spellingPrompt(memoryTracker), noThinkingTime);

    assertThat(indexWithBaseThinkingTime, equalTo(memoryTracker.getForgettingCurveIndex()));
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.answerSpelling(recallPrompt, spellingAnswer("x")));
  }

  @Test
  void shouldValidateRecallPromptIsSpellingType() {
    RecallPrompt mcqPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
    assertThrows(
        IllegalArgumentException.class,
        () -> controller.answerSpelling(mcqPrompt, spellingAnswer(answerNote.getTitle())));
  }

  @Test
  void shouldNotPopulateAccidentalMatchFieldsOnCorrectSpellingAnswer()
      throws UnexpectedNoAccessRightException {
    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
    assertNull(answerResult.getAnswer().getOutcome());
    assertNull(answerResult.getMatchedNotes());
  }

  @Nested
  class WrongAnswer {
    @BeforeEach
    void setup() {
      answerDTO = spellingAnswer("wrong");
    }

    @Test
    void shouldValidateTheWrongAnswer() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer oldRecallCount = memoryTracker.getRecallCount();
      AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);
      assertFalse(answerResult.getAnswer().getCorrect());
      assertNull(answerResult.getAnswer().getOutcome());
      assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
    }

    @Test
    void shouldMoveLastRecalledAtToGradeTimeAndReduceStrength()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldRepeatInTwelveHours() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      controller.answerSpelling(recallPrompt, answerDTO);
      assertThat(
          memoryTracker.getNextRecallAt(),
          equalTo(
              TimestampOperations.addHoursToTimestamp(
                  testabilitySettings.getCurrentUTCTimestamp(), 12)));
    }
  }
}
