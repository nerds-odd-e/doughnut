package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class RecallsControllerTestBase extends ControllerTestBase {

  @Autowired RecallsController controller;

  @BeforeEach
  void setupRecallsTests() {
    currentUser.setUser(makeMe.aUser().please());
  }

  protected Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  protected MemoryTracker dueTracker(Note note, Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerFor(note).nextRecallAt(nextRecallAt).please();
  }

  protected MemoryTracker dueTracker(Timestamp nextRecallAt) {
    return makeMe.aMemoryTrackerBy(currentUser.getUser()).nextRecallAt(nextRecallAt).please();
  }

  protected Notebook spanishConversationNotebook() {
    return spanishConversationNotebook(currentUser.getUser());
  }

  protected Notebook spanishConversationNotebook(User owner) {
    return makeMe.aNotebook().creatorAndOwner(owner).name("Spanish conversation").please();
  }
}
