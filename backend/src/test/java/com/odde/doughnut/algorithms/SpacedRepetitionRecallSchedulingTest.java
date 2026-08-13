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
  @Test
  void correctRecallIntervalIsIndependentOfPersistedDueProjection() {
    MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();
    User user = makeMe.aUser().withSpaceIntervals("3, 6, 9, 12, 15").inMemoryPlease();
    Note note = makeMe.aNote().inMemoryPlease();
    MemoryTracker earlierProjection =
        makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRecall(3).inMemoryPlease();
    MemoryTracker laterProjection =
        makeMe.aMemoryTrackerFor(note).by(user).afterNthStrictRecall(3).inMemoryPlease();
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
}
