package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class LearningSessionRecordTutorFeedbackTests extends LearningSessionControllerTestBase {

  @Test
  void recordingTutorFeedbackAdvancesScheduleFromUnset() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    assertThat(fixture.holaTracker().getDifficulty(), nullValue());

    controller.record(
        recordRequest(fixture.notebook(), learningSessionReport("Hola", 3)), "Asia/Shanghai");

    MemoryTracker hola = fixture.holaTracker();
    assertThat(hola.getDifficulty(), notNullValue());
    assertThat(hola.getStability(), greaterThan(0f));
    assertThat(hola.getLastRecalledAt(), equalTo(dayTwo));
    assertThat(
        hola.getNextRecallAt(),
        equalTo(
            TimestampOperations.addHoursToTimestamp(
                hola.getLastRecalledAt(), Math.round(hola.getStability()))));
  }
}
