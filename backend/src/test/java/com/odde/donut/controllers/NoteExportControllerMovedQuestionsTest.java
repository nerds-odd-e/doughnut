package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteExportControllerMovedQuestionsTest extends ControllerTestBase {
  @Autowired NoteExportController controller;

  @Test
  void exportsTheMovedTrackersQuestionAndOwnChoiceOnItsCurrentNote()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.aUser().please());
    Note original = makeMe.aNote("Original").notebookOwnedBy(currentUser.getUser()).please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(original).please();
    makeMe
        .aRecallPrompt()
        .forMemoryTracker(tracker)
        .withMcq(
            makeMe
                .anMcq()
                .stem("Question retained by its tracker")
                .choices("key", "second", "selected-third")
                .correctAnswerIndex(0)
                .please())
        .answerChoiceIndex(2)
        .please();

    Note current = reifyTrackedProperty(tracker, "Target");
    String markdown =
        controller.getAiContextMarkdown(current, 5000, true, null, null, null).markdown();

    assertThat(markdown, containsString("1. Question retained by its tracker"));
    assertThat(markdown, containsString("Learner's selected answer: selected-third"));
  }
}
