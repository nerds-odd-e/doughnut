package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.controllers.dto.UpdateMemoryTrackerPropertyKeyDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
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

  RecallPrompt promptFor(MemoryTracker tracker, Note note) {
    return makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
  }

  RecallPrompt answeredPromptFor(MemoryTracker tracker, Note note) {
    return makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .please();
  }

  void assertConversationHasNoRecallPrompt(Conversation conversation) {
    assertThat(
        conversation.getSubject() == null || conversation.getSubject().getRecallPrompt() == null,
        is(true));
  }

  MemoryTracker ownedTracker() {
    return ownedTracker(ownedNote());
  }

  void addRecallLogs(MemoryTracker tracker, Grade grade, int count, Timestamp day) {
    for (int i = 0; i < count; i++) {
      makeMe.aRecallLogFor(tracker).grade(grade).recordedAt(day).please();
    }
  }

  void addConfusionRecallLogs(MemoryTracker tracker, int count, Timestamp day) {
    for (int i = 0; i < count; i++) {
      makeMe.aRecallLogFor(tracker).confusion().recordedAt(day).please();
    }
  }

  UpdateMemoryTrackerPropertyKeyDTO renameTo(String propertyKey) {
    UpdateMemoryTrackerPropertyKeyDTO dto = new UpdateMemoryTrackerPropertyKeyDTO();
    dto.setPropertyKey(propertyKey);
    return dto;
  }
}
