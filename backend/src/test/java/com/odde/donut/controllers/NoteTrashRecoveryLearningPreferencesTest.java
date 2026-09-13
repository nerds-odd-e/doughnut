package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteTrashRecoveryLearningPreferencesTest extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void trashInactivatesTrackersAndUndoTrashReactivatesThemWhilePreservingPreferences()
      throws UnexpectedNoAccessRightException {
    Folder originalFolder =
        makeMe
            .aFolder()
            .notebook(makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please())
            .name("Topics")
            .please();
    Note note = makeMe.aNote("Subject").folder(originalFolder).please();
    MemoryTracker tracker =
        makeMe
            .aMemoryTrackerFor(note)
            .spelling()
            .propertyKey("spelling")
            .stabilityAndNextRecallAt(12.5f)
            .difficulty(0.3f)
            .please();
    Integer trackerId = tracker.getId();

    controller.trashNote(note, leaveDeadLinks());

    MemoryTracker trashedTracker = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(trashedTracker.isActive(), equalTo(false));
    assertThat(trashedTracker.getNote().isAvailable(), equalTo(false));

    controller.undoTrashNote(note, undoTo("Subject", originalFolder));

    MemoryTracker recoveredTracker = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(recoveredTracker.isActive(), equalTo(true));
    assertThat(recoveredTracker.getNote().isAvailable(), equalTo(true));
    assertThat(recoveredTracker.getType(), equalTo(MemoryTrackerType.SPELLING));
    assertThat(recoveredTracker.getPropertyKey(), equalTo("spelling"));
    assertThat(recoveredTracker.getStability(), equalTo(12.5f));
    assertThat(recoveredTracker.getDifficulty(), equalTo(0.3f));
    assertThat(recoveredTracker.getRemovedFromTracking(), equalTo(false));
  }

  @Test
  void trashAndUndoTrashPreservesRemovedFromTrackingTrackers()
      throws UnexpectedNoAccessRightException {
    Folder originalFolder =
        makeMe
            .aFolder()
            .notebook(makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please())
            .name("Topics")
            .please();
    Note note = makeMe.aNote("Subject").folder(originalFolder).please();
    MemoryTracker stopped = makeMe.aMemoryTrackerFor(note).removedFromTracking().please();
    Integer stoppedId = stopped.getId();

    controller.trashNote(note, leaveDeadLinks());
    controller.undoTrashNote(note, undoTo("Subject", originalFolder));

    MemoryTracker recovered = memoryTrackerRepository.findById(stoppedId).orElseThrow();
    assertThat(recovered.getRemovedFromTracking(), equalTo(true));
    assertThat(recovered.isActive(), equalTo(false));
  }
}
