package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.AnsweredQuestion;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.entities.AnswerOutcome;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecallPromptAccidentalMatchGradingTests extends RecallPromptControllerTestBase {

  Note secondNote;
  MemoryTracker memoryTracker;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO;

  @BeforeEach
  void setup() {
    Note answerNote = ownedNote();
    memoryTracker = ownedSpellingTracker(answerNote);
    recallPrompt = spellingPrompt(memoryTracker);
    secondNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    answerDTO = spellingAnswer(secondNote.getTitle());
  }

  @Test
  void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteTitle()
      throws UnexpectedNoAccessRightException {
    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertFalse(answerResult.getAnswer().getCorrect());
    assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
    assertThat(
        answerResult.getAnswer().getMatchedNoteId(), equalTo(secondNote.getId().longValue()));
    assertThat(answerResult.getMatchedNotes(), hasSize(1));
    assertThat(answerResult.getMatchedNotes().getFirst().getId(), equalTo(secondNote.getId()));
    assertThat(
        answerResult.getMatchedNotes().getFirst().getTitle(), equalTo(secondNote.getTitle()));
  }

  @Test
  void accidentalMatchLeavesAnAgainRecallLog() throws UnexpectedNoAccessRightException {
    controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        memoryTrackerController.getRecallLogs(memoryTracker).get(0).getProductOutcome(),
        is(ProductOutcome.AGAIN));
  }

  @Test
  void shouldIncludeTitleAndAliasMatchesInMatchedNotesOrderedById()
      throws UnexpectedNoAccessRightException {
    String shared = "TitlePreferredMatch";
    Note noteA = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(shared).please();
    Note noteB =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .title("AliasOnlyNoteTitle")
            .aliases(shared)
            .please();
    answerDTO = spellingAnswer(shared);

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    int minId = Math.min(noteA.getId(), noteB.getId());
    int maxId = Math.max(noteA.getId(), noteB.getId());
    assertThat(
        answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
        contains(minId, maxId));
    assertThat(answerResult.getAnswer().getMatchedNoteId(), equalTo((long) minId));
  }

  @Test
  void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAnotherReadableNoteAlias()
      throws UnexpectedNoAccessRightException {
    Note aliasBearingNote =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .aliases("AccidentalAliasMatch")
            .please();
    answerDTO = spellingAnswer("AccidentalAliasMatch");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
    assertThat(
        answerResult.getAnswer().getMatchedNoteId(), equalTo(aliasBearingNote.getId().longValue()));
  }

  @Test
  void shouldNotAccidentalMatchViaWikiLinkOverlapAliasItem()
      throws UnexpectedNoAccessRightException {
    makeMe
        .aNote()
        .notebookOwnedBy(currentUser.getUser())
        .overlapWikiLink("OverlapTargetTitle")
        .please();
    answerDTO = spellingAnswer("OverlapTargetTitle");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertNull(answerResult.getAnswer().getOutcome());
  }

  @Test
  void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAliasAfterTrim()
      throws UnexpectedNoAccessRightException {
    makeMe.aNote().notebookOwnedBy(currentUser.getUser()).aliases("AccidentalAliasMatch").please();
    answerDTO = spellingAnswer("  AccidentalAliasMatch  ");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
  }
}
