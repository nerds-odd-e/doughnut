package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.entities.ForgettingCurve;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptAnswerControllerTest extends RecallPromptControllerTestBase {
  @Autowired ObjectMapper objectMapper;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerDTO answerDTO;

  @BeforeEach
  void setup() {
    Note answerNote = ownedNote();
    memoryTracker = ownedTracker(answerNote);
    recallPrompt = mcqPrompt(memoryTracker, answerNote);
    answerDTO = choiceAnswer(0);
  }

  @Test
  void shouldValidateTheAnswerAndUpdateMemoryTracker() throws UnexpectedNoAccessRightException {
    Integer oldRecallCount = memoryTracker.getRecallCount();
    AnsweredQuestion answerResult = controller.answer(recallPrompt, answerDTO);
    assertThat(answerResult.getAnswer().getCorrect(), is(true));
    assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
  }

  @Test
  void shouldUpdateLinkedPropertyMemoryTrackerWhenAnsweringPropertyQuestion()
      throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker noteLevelTracker = ownedTracker(note);
    MemoryTracker propertyTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .propertyKey("topic")
            .stabilityAndNextRecallAt(200.0f)
            .please();
    testabilitySettings.timeTravelTo(propertyTracker.getNextRecallAt());

    Integer noteLevelRecallCountBefore = noteLevelTracker.getRecallCount();
    Float noteLevelStabilityBefore = noteLevelTracker.getStability();
    Integer propertyRecallCountBefore = propertyTracker.getRecallCount();

    controller.answer(mcqPrompt(propertyTracker, note), answerDTO);

    assertThat(noteLevelTracker.getRecallCount(), equalTo(noteLevelRecallCountBefore));
    assertThat(noteLevelTracker.getStability(), equalTo(noteLevelStabilityBefore));
    assertThat(propertyTracker.getRecallCount(), greaterThan(propertyRecallCountBefore));
  }

  @Test
  void shouldExposeAnsweredMcqUnderMcqField() throws Exception {
    AnsweredQuestion answerResult = controller.answer(recallPrompt, answerDTO);
    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(answerResult));
    assertThat(json.has("mcq"), is(true));
    assertThat(json.has("predefinedQuestion"), is(false));
  }

  @Test
  void shouldSaveThinkingTimeMs() throws UnexpectedNoAccessRightException {
    answerDTO.setThinkingTimeMs(5000);
    assertThat(
        controller.answer(recallPrompt, answerDTO).getAnswer().getThinkingTimeMs(), equalTo(5000));
  }

  @Test
  void shouldNotIncreaseStabilityIfRepeatImmediately() throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
    Float oldStability = memoryTracker.getStability();
    controller.answer(recallPrompt, answerDTO);
    assertThat(memoryTracker.getStability(), equalTo(oldStability));
  }

  @Test
  void shouldIncreaseStability() throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    Float oldStability = memoryTracker.getStability();
    controller.answer(recallPrompt, answerDTO);
    assertThat(memoryTracker.getStability(), greaterThan(oldStability));
    assertThat(
        memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
  }

  @Test
  void fastAnswer_shouldIncreaseStabilityMoreThanSlowAnswer()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    Float baseStability = memoryTracker.getStability();
    Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

    answerDTO.setThinkingTimeMs(10000);
    controller.answer(recallPrompt, answerDTO);
    Float stabilityWithFastAnswer = memoryTracker.getStability();

    memoryTracker.setStability(baseStability);
    memoryTracker.setLastRecalledAt(baseLastRecalledAt);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    answerDTO.setThinkingTimeMs(40000);
    controller.answer(mcqPrompt(memoryTracker, memoryTracker.getNote()), answerDTO);

    assertThat(stabilityWithFastAnswer, greaterThan(memoryTracker.getStability()));
  }

  @Test
  void answerWithBaseThinkingTime_shouldHaveNoThinkingTimeAdjustment()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
    Float baseStability = memoryTracker.getStability();
    Timestamp baseLastRecalledAt = memoryTracker.getLastRecalledAt();

    answerDTO.setThinkingTimeMs(ForgettingCurve.BASE_THINKING_TIME_MS);
    controller.answer(recallPrompt, answerDTO);
    Float stabilityWithBaseThinkingTime = memoryTracker.getStability();

    memoryTracker.setStability(baseStability);
    memoryTracker.setLastRecalledAt(baseLastRecalledAt);
    memoryTracker.setNextRecallAt(memoryTracker.calculateNextRecallAt());
    answerDTO.setThinkingTimeMs(null);
    controller.answer(mcqPrompt(memoryTracker, memoryTracker.getNote()), answerDTO);

    assertThat(stabilityWithBaseThinkingTime, equalTo(memoryTracker.getStability()));
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class, () -> controller.answer(recallPrompt, choiceAnswer(0)));
  }

  @Test
  void shouldNotBeAbleToAnswerForOthersMemoryTracker() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(othersTracker)
            .withMcqForNote(othersTracker.getNote())
            .please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.answer(othersPrompt, answerDTO));
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
      AnsweredQuestion answerResult = controller.answer(recallPrompt, answerDTO);
      assertThat(answerResult.getAnswer().getCorrect(), is(false));
      assertThat(memoryTracker.getRecallCount(), greaterThan(oldRecallCount));
    }

    @Test
    void shouldMoveLastRecalledAtToGradeTimeAndReduceStability()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Float oldStability = memoryTracker.getStability();
      controller.answer(recallPrompt, answerDTO);
      assertThat(memoryTracker.getStability(), lessThan(oldStability));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }
  }
}
