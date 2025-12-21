package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RecallServiceWithSpacedRepetitionAlgorithmTest {
  @Autowired MakeMe makeMe;
  @Autowired UserService userService;
  User user;
  User anotherUser;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").please();
    anotherUser = makeMe.aUser().please();
  }

  @Nested
  class WhenThereIsOneNote {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(user).please();
    }

    @Test
    void whenThereIsNoReviewedNotesForUser() {
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(anotherUser).please();
      assertThat(getOneMemoryTrackerNeedToRepeat(daysAfterBase(memoryTracker, 1)), is(nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
      "1,   0, false",
      "1,   1, true",
      "1,   2, true",
      "1,  10, true",
      "2,   0, false",
      "2,   1, false",
      "2,   2, true",
      "2,  10, true",
      "3,   0, false",
      "3,   1, false",
      "3,   3, false",
      "3,   4, true",
    })
    void whenThereIsOneReviewedNotesForUser(
        Integer repetitionDone, Integer reviewDay, Boolean expectedToRepeat) {
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRepetition(repetitionDone).please();
      MemoryTracker mostUrgentMemoryTracker =
          getOneMemoryTrackerNeedToRepeat(daysAfterBase(memoryTracker, reviewDay));
      assertThat(mostUrgentMemoryTracker != null, is(expectedToRepeat));
    }

    @Nested
    class ReviewTimeIsAlignedByHalfADay {
      @ParameterizedTest
      @CsvSource({
        "9,  6,    true",
        "16, 0,    false",
        "16, 15,   true",
        "16, 17,   true",
      })
      void atHourInTheNextDay(
          Integer lastRepeatHour, Integer currentHour, Boolean expectedToRepeat) {
        MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
        memoryTracker.setNextRecallAt(
            makeMe.aTimestamp().of(2, lastRepeatHour).fromShanghai().please());
        final Timestamp timestamp = makeMe.aTimestamp().of(2, currentHour).fromShanghai().please();
        MemoryTracker mostUrgentMemoryTracker = getOneMemoryTrackerNeedToRepeat(timestamp);
        assertThat(mostUrgentMemoryTracker != null, is(expectedToRepeat));
      }
    }

    @Nested
    class EarlyAndLateReview {
      @ParameterizedTest
      @CsvSource({
        "0, 0,  100.0",
        "0, 1,  110.0",
        "2, -1,  115.0",
        "2, 0, 120.0",
        "2, 1, 115.0",
        "2, 100, 100.0",
      })
      void aMemoryTrackerHasBeenReviewedStrictly(
          int ntimes, Integer daysDelay, float expectedForgettingCurveIndex) {
        MemoryTracker memoryTracker =
            makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRepetition(ntimes).please();
        Timestamp currentUTCTimestamp =
            TimestampOperations.addHoursToTimestamp(
                memoryTracker.getNextRecallAt(), daysDelay * 24);
        memoryTracker.markAsRepeated(currentUTCTimestamp, true, null);
        assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(expectedForgettingCurveIndex));
      }
    }
  }

  private MemoryTracker getOneMemoryTrackerNeedToRepeat(Timestamp timestamp) {
    return userService
        .getMemoryTrackersNeedToRepeat(user, timestamp, ZoneId.of("Asia/Shanghai"))
        .findFirst()
        .orElse(null);
  }

  private Timestamp daysAfterBase(MemoryTracker memoryTracker, Integer reviewDay) {
    return TimestampOperations.addHoursToTimestamp(
        memoryTracker.getLastRecalledAt(), reviewDay * 24);
  }
}
