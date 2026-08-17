package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemoryTrackerRecallPromptHistoryCorrectnessControllerTest
    extends MemoryTrackerControllerTestBase {
  @Autowired RecallPromptController recallPromptController;

  private void gradeMcq(RecallPrompt prompt, int choiceIndex)
      throws UnexpectedNoAccessRightException {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(choiceIndex);
    recallPromptController.answer(prompt, dto);
  }

  private void gradeSpelling(RecallPrompt prompt, String spelling)
      throws UnexpectedNoAccessRightException {
    AnswerSpellingDTO dto = new AnswerSpellingDTO();
    dto.setSpellingAnswer(spelling);
    recallPromptController.answerSpelling(prompt, dto);
  }

  private Boolean reloadedHistoryCorrect(MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    Integer id = tracker.getId();
    makeMe.entityPersister.flushAndClear();
    return controller
        .getRecallPrompts(makeMe.entityPersister.find(MemoryTracker.class, id))
        .get(0)
        .getAnswer()
        .getCorrect();
  }

  @Test
  void correctMcqHistoryShowsCorrectAfterReload() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    gradeMcq(promptFor(tracker, note), 0);

    assertThat(reloadedHistoryCorrect(tracker), is(true));
  }

  @Test
  void incorrectMcqHistoryShowsIncorrectAfterReload() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    gradeMcq(promptFor(tracker, note), 1);

    assertThat(reloadedHistoryCorrect(tracker), is(false));
  }

  @Test
  void overlapHistoryShowsCorrectAfterReload() throws UnexpectedNoAccessRightException {
    Note partner =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Shared Title").please();
    Note reviewed =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("Shared Title")
            .overlapPartner(partner)
            .please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(reviewed).spelling().please();
    gradeSpelling(
        makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please(), "Shared Title");

    assertThat(reloadedHistoryCorrect(tracker), is(true));
  }
}
