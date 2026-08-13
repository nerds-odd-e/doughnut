package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class SpacedRepetitionRecallSchedulingTest {
  private final MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
  private final User user = makeMe.aUser().withSpaceIntervals("3, 6, 9, 12, 15").inMemoryPlease();
  private final Note note = makeMe.aNote().inMemoryPlease();

  private MemoryTracker aMemoryTrackerAfterThreeStrictRecalls() {
    return makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRecall(3).inMemoryPlease();
  }

  @Test
  void correctRecallIntervalIsIndependentOfPersistedDueProjection() {
    MemoryTracker earlierProjection = aMemoryTrackerAfterThreeStrictRecalls();
    MemoryTracker laterProjection = aMemoryTrackerAfterThreeStrictRecalls();
    Timestamp gradeTime =
        TimestampOperations.addHoursToTimestamp(earlierProjection.getLastRecalledAt(), 24);
    earlierProjection.setNextRecallAt(TimestampOperations.addHoursToTimestamp(gradeTime, -24));
    laterProjection.setNextRecallAt(TimestampOperations.addHoursToTimestamp(gradeTime, 48));

    earlierProjection.recalledSuccessfully(gradeTime, null);
    laterProjection.recalledSuccessfully(gradeTime, null);

    long earlierInterval =
        TimestampOperations.getDiffInHours(earlierProjection.getNextRecallAt(), gradeTime);
    long laterInterval =
        TimestampOperations.getDiffInHours(laterProjection.getNextRecallAt(), gradeTime);
    assertThat(earlierInterval, equalTo(laterInterval));
  }

  @Test
  void correctRecallIntervalUsesWholeElapsedHours() {
    MemoryTracker wholeHourRecall = aMemoryTrackerAfterThreeStrictRecalls();
    MemoryTracker recallWithSubHourRemainder = aMemoryTrackerAfterThreeStrictRecalls();
    Timestamp wholeHourGradeTime =
        TimestampOperations.addHoursToTimestamp(wholeHourRecall.getLastRecalledAt(), 300);
    Timestamp gradeTimeWithSubHourRemainder =
        Timestamp.from(wholeHourGradeTime.toInstant().plusSeconds(30 * 60));

    wholeHourRecall.recalledSuccessfully(wholeHourGradeTime, null);
    recallWithSubHourRemainder.recalledSuccessfully(gradeTimeWithSubHourRemainder, null);

    long wholeHourInterval =
        TimestampOperations.getDiffInHours(wholeHourRecall.getNextRecallAt(), wholeHourGradeTime);
    long subHourRemainderInterval =
        TimestampOperations.getDiffInHours(
            recallWithSubHourRemainder.getNextRecallAt(), gradeTimeWithSubHourRemainder);
    assertThat(subHourRemainderInterval, equalTo(wholeHourInterval));
    assertThat(wholeHourRecall.getLastRecalledAt(), equalTo(wholeHourGradeTime));
    assertThat(
        recallWithSubHourRemainder.getLastRecalledAt(), equalTo(gradeTimeWithSubHourRemainder));
  }

  @Test
  void correctRecallAfterFailureUsesElapsedHoursSinceFailure() {
    MemoryTracker memoryTracker = aMemoryTrackerAfterThreeStrictRecalls();
    Timestamp failureTime =
        TimestampOperations.addHoursToTimestamp(memoryTracker.getLastRecalledAt(), 300);
    Timestamp correctGradeTime = TimestampOperations.addHoursToTimestamp(failureTime, 24);

    memoryTracker.markAsRecalled(failureTime, false, null);
    memoryTracker.markAsRecalled(correctGradeTime, true, null);

    long intervalAfterCorrectRecall =
        TimestampOperations.getDiffInHours(memoryTracker.getNextRecallAt(), correctGradeTime);
    assertThat(intervalAfterCorrectRecall, equalTo(96L));
  }
}
