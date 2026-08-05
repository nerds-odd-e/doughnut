package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallPromptAccidentalMatchGradingTests extends ControllerTestBase {

  @Autowired RecallPromptController controller;

  Note secondNote;
  RecallPrompt recallPrompt;
  AnswerSpellingDTO answerDTO = new AnswerSpellingDTO();

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    Note answerNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).rememberSpelling().please();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(answerNote).spelling().please();
    recallPrompt = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();
    secondNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Another Note Title").please();
    answerDTO.setSpellingAnswer(secondNote.getTitle());
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
  void shouldPreferLowestNoteIdWhenMultipleReadableNotesShareTitle()
      throws UnexpectedNoAccessRightException {
    Note thirdNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(secondNote.getTitle()).please();

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(
        answerResult.getMatchedNotes().stream().map(NoteTopology::getId).toList(),
        contains(secondNote.getId(), thirdNote.getId()));
    assertThat(
        answerResult.getAnswer().getMatchedNoteId(), equalTo(secondNote.getId().longValue()));
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
    answerDTO.setSpellingAnswer(shared);

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
    answerDTO.setSpellingAnswer("AccidentalAliasMatch");

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
    answerDTO.setSpellingAnswer("OverlapTargetTitle");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertNull(answerResult.getAnswer().getOutcome());
  }

  @Test
  void shouldGradeAsAccidentalMatchWhenWrongAnswerMatchesAliasAfterTrim()
      throws UnexpectedNoAccessRightException {
    makeMe.aNote().notebookOwnedBy(currentUser.getUser()).aliases("AccidentalAliasMatch").please();
    answerDTO.setSpellingAnswer("  AccidentalAliasMatch  ");

    AnsweredQuestion answerResult = controller.answerSpelling(recallPrompt, answerDTO);

    assertThat(answerResult.getAnswer().getOutcome(), is(AnswerOutcome.ACCIDENTAL_MATCH));
  }
}
