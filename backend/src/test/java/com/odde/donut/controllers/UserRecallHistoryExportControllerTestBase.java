package com.odde.donut.controllers;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class UserRecallHistoryExportControllerTestBase extends ControllerTestBase {
  @Autowired UserController controller;
  @Autowired RecallPromptController recallPromptController;

  Note note;
  MemoryTracker tracker;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = ownedNote("Some Note");
    tracker = makeMe.aMemoryTrackerFor(note).please();
  }

  Note ownedNote(String title) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(title).please();
  }

  RecallPrompt answerMcqAtDay(int day) throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(day, 8).please());
    RecallPrompt prompt = makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    recallPromptController.answer(prompt, choiceAnswer(0));
    return prompt;
  }
}
