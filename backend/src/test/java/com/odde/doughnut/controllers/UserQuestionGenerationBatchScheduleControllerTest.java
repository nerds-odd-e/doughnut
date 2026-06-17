package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchUserScheduleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserQuestionGenerationBatchScheduleControllerTest extends ControllerTestBase {
  @Autowired UserController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void getsQuestionGenerationBatchScheduleForCurrentUser() {
    Timestamp currentTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0));
    testabilitySettings.timeTravelTo(currentTime);
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    var memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(currentUser.getUser())
            .nextRecallAt(TimestampOperations.addHoursToTimestamp(currentTime, 24))
            .please();
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 8, 30)))
        .please();

    QuestionGenerationBatchUserScheduleDTO schedule =
        controller.getQuestionGenerationBatchSchedule();

    assertThat(schedule.getNextScheduledAt(), equalTo(currentTime));
  }
}
