package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerThresholdControllerTest extends MemoryTrackerControllerTestBase {

  @ParameterizedTest
  @CsvSource({"4, false", "5, true", "6, true"})
  void reportsThresholdFromWrongAnswerCount(int wrongAnswers, boolean exceeded)
      throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    addWrongAnswers(tracker, note, wrongAnswers, day1);

    testabilitySettings.timeTravelTo(day1);
    assertThat(controller.getThresholdExceeded(tracker).thresholdExceeded(), equalTo(exceeded));
  }

  @Test
  void scopesWrongAnswersPerMemoryTrackerNotPerNote() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker noteLevelTracker = ownedTracker(note);
    MemoryTracker propertyTracker = makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    addWrongAnswers(propertyTracker, note, 5, day1);

    testabilitySettings.timeTravelTo(day1);
    assertThat(
        controller.getThresholdExceeded(noteLevelTracker).thresholdExceeded(), equalTo(false));
    assertThat(controller.getThresholdExceeded(propertyTracker).thresholdExceeded(), equalTo(true));
  }

  @Test
  void shouldNotBeAbleToGetForOthersMemoryTracker() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getThresholdExceeded(memoryTracker));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(
        ResponseStatusException.class, () -> controller.getThresholdExceeded(memoryTracker));
  }
}
