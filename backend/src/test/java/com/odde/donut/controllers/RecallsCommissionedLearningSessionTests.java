package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.DueMemoryTrackers;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class RecallsCommissionedLearningSessionTests extends RecallsControllerTestBase {

  @Test
  void shouldListDueCommissionedTrackersSeparatelyFromOrdinaryRecall() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    Note note = makeMe.aNote().notebook(spanishConversationNotebook()).please();
    MemoryTracker commissioned =
        makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(currentTime).please();

    DueMemoryTrackers dueMemoryTrackers = controller.recalling("Asia/Shanghai", 0);

    assertThat(dueMemoryTrackers.getToRepeat(), hasSize(0));
    assertThat(dueMemoryTrackers.getDueCommissioned(), hasSize(1));
    assertEquals(
        commissioned.getId(), dueMemoryTrackers.getDueCommissioned().get(0).getMemoryTrackerId());
    assertEquals(
        note.getNotebook().getId(), dueMemoryTrackers.getDueCommissioned().get(0).getNotebookId());
    assertEquals(
        "Spanish conversation", dueMemoryTrackers.getDueCommissioned().get(0).getNotebookName());
  }
}
