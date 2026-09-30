package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FollowPropertyValueDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class MemoryTrackerFollowPropertyValueControllerTest extends MemoryTrackerControllerTestBase {

  private FollowPropertyValueDTO follow(String propertyKey, String propertyValue) {
    FollowPropertyValueDTO dto = new FollowPropertyValueDTO();
    dto.setPropertyKey(propertyKey);
    dto.setPropertyValue(propertyValue);
    return dto;
  }

  @Test
  void aSingleValueTrackerFollowsItsValueKeepingHistoryAndSchedule()
      throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(note)
            .propertyKey("example of")
            .afterNthStrictRecall(2)
            .recallCount(2)
            .please();
    Integer recallCount = tracker.getRecallCount();
    Float stability = tracker.getStability();
    Timestamp nextRecallAt = tracker.getNextRecallAt();

    controller.followPropertyValue(note, follow("example of", "[[run]]"));

    MemoryTracker followed = makeMe.refresh(tracker);
    assertThat(followed.getPropertyKey(), equalTo("example of"));
    assertThat(followed.getPropertyValue(), equalTo("[[run]]"));
    assertThat(followed.getRecallCount(), equalTo(recallCount));
    assertThat(followed.getStability(), equalTo(stability));
    assertThat(followed.getNextRecallAt(), equalTo(nextRecallAt));
    assertThat(controller.getRecallHistory(followed), hasSize(2));
  }

  @Test
  void everyLearnersSingleValueTrackerOfThatKeyFollows() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker another =
        makeMe
            .aMemoryTrackerFor(note)
            .by(makeMe.aUser().please())
            .propertyKey("example of")
            .please();

    controller.followPropertyValue(note, follow("example of", "[[run]]"));

    assertThat(makeMe.refresh(another).getPropertyValue(), equalTo("[[run]]"));
  }

  @Test
  void trackersOfOtherKeysOrValuesStay() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker otherKey = makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();
    MemoryTracker listValue =
        makeMe
            .aMemoryTrackerFor(note)
            .by(makeMe.aUser().please())
            .propertyKey("example of")
            .propertyValue("[[past tense]]")
            .please();

    controller.followPropertyValue(note, follow("example of", "[[run]]"));

    assertThat(makeMe.refresh(otherKey).getPropertyValue(), equalTo(""));
    assertThat(makeMe.refresh(listValue).getPropertyValue(), equalTo("[[past tense]]"));
  }

  @Test
  void onlyAnAuthorOfTheNoteCanMoveItsTrackers() {
    Note note = makeMe.aNote().please();
    makeMe.aMemoryTrackerFor(note).propertyKey("example of").please();

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.followPropertyValue(note, follow("example of", "[[run]]")));
  }
}
