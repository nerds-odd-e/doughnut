package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.UpdateMemoryTrackerPropertyKeyDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.NoteService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class MemoryTrackerControllerTestBase extends ControllerTestBase {
  @Autowired MemoryTrackerController controller;
  @Autowired NoteService noteService;

  @BeforeEach
  void setupCurrentUser() {
    currentUser.setUser(makeMe.aUser().please());
  }

  Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  MemoryTracker ownedTracker(Note note) {
    return makeMe.aMemoryTrackerFor(note).please();
  }

  MemoryTracker ownedTracker() {
    return ownedTracker(ownedNote());
  }

  void addWrongAnswers(MemoryTracker tracker, Note note, int count, Timestamp day) {
    for (int i = 0; i < count; i++) {
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(tracker)
          .answerChoiceIndex(1)
          .answerTimestamp(day)
          .please();
    }
  }

  UpdateMemoryTrackerPropertyKeyDTO renameTo(String propertyKey) {
    UpdateMemoryTrackerPropertyKeyDTO dto = new UpdateMemoryTrackerPropertyKeyDTO();
    dto.setPropertyKey(propertyKey);
    return dto;
  }
}
