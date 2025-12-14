package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.DueMemoryTrackers;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class RecallsControllerTests extends ControllerTestBase {
  @Autowired RecallsController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  RecallsController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  @Nested
  class Repeat {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().recalling("Asia/Shanghai", null));
    }

    @ParameterizedTest
    @CsvSource(
        useHeadersInDisplayName = true,
        delimiter = '|',
        textBlock =
            """
                next review at (in hours) | timezone     | expected count
                #------------------------------------------------------------
                -1                        | Asia/Tokyo   | 1
                0                         | Asia/Tokyo   | 1
                4                         | Asia/Tokyo   | 0
                4                         | Europe/Paris | 1
                12                        | Europe/Paris | 0
                """)
    void shouldGetMemoryTrackersBasedOnTimezone(
        int nextRecallAtHours, String timezone, int expectedCount) {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe
          .aMemoryTrackerBy(currentUser.getUser())
          .nextRecallAt(TimestampOperations.addHoursToTimestamp(currentTime, nextRecallAtHours))
          .please();
      DueMemoryTrackers dueMemoryTrackers = controller.recalling(timezone, null);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(expectedCount));
    }

    @Test
    void shouldIncludeRecallStatusInDueMemoryTrackers() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.getToRepeat().size());
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
      // currentTime is 1989-01-01 00:00:00 UTC, which is 1989-01-01 08:00:00 in Asia/Shanghai
      // Since hour < 12, alignByHalfADay returns same day at 12:00:00 Asia/Shanghai = 04:00:00 UTC
      Timestamp expectedEndAt = TimestampOperations.addHoursToTimestamp(currentTime, 4);
      assertEquals(expectedEndAt, dueMemoryTrackers.getCurrentRecallWindowEndAt());
    }

    @ParameterizedTest
    @CsvSource({
      "0,  12", "6,  6", "11, 1", "12, 12", "18, 6",
    })
    void shouldSetCurrentRecallWindowEndAtAlignedByHalfADay(
        int currentHour, int expectedHoursToAdd) {
      Timestamp currentTime = makeMe.aTimestamp().of(1, currentHour).fromShanghai().please();
      testabilitySettings.timeTravelTo(currentTime);
      makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(currentTime).please();

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      Timestamp expectedEndAt =
          TimestampOperations.addHoursToTimestamp(currentTime, expectedHoursToAdd);
      assertEquals(expectedEndAt, dueMemoryTrackers.getCurrentRecallWindowEndAt());
      // Verify it's not affected by dueInDays
      DueMemoryTrackers dueMemoryTrackersWithDays = controller.recalling("Asia/Shanghai", 3);
      assertEquals(expectedEndAt, dueMemoryTrackersWithDays.getCurrentRecallWindowEndAt());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe
          .aMemoryTrackerFor(activeNote)
          .by(currentUser.getUser())
          .nextRecallAt(currentTime)
          .please();
      makeMe
          .aMemoryTrackerFor(deletedNote)
          .by(currentUser.getUser())
          .nextRecallAt(currentTime)
          .please();

      deletedNote.setDeletedAt(currentTime);
      makeMe.entityPersister.merge(deletedNote);

      DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

      assertEquals(1, dueMemoryTrackers.getToRepeat().size());
      assertEquals(1, dueMemoryTrackers.totalAssimilatedCount);
      assertThat(dueMemoryTrackers.getToRepeat(), hasSize(1));
    }
  }
}
