package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRecallInfo;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerNoteInfoTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getNoteInfo(note));
  }

  @Test
  void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    makeMe.aSubscription().forUser(currentUser.getUser()).forNotebook(note.getNotebook()).please();
    makeMe.refresh(currentUser.getUser());
    assertThat(controller.getNoteInfo(note), notNullValue());
  }

  @Test
  void shouldIncludeSkippedMemoryTrackersInNoteInfo() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(note).please();
    makeMe.aMemoryTrackerFor(note).spelling().removedFromTracking().please();

    NoteRecallInfo noteRecallInfo = controller.getNoteInfo(note);
    assertThat(noteRecallInfo.getMemoryTrackers(), hasSize(2));
    assertThat(
        noteRecallInfo.getMemoryTrackers(), hasItem(hasProperty("removedFromTracking", is(true))));
  }

  @Test
  void shouldIncludeSequenceSkipFactWhenSkipRowExists() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.anAssimilationSequenceSkipFor(note).please();

    NoteRecallInfo noteRecallInfo = controller.getNoteInfo(note);

    assertThat(noteRecallInfo.getSkippedPropertyKeys(), contains(""));
  }

  @Test
  void shouldIncludeSkippedPropertyKeysWhenPropertySkipRowExists()
      throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.anAssimilationSequenceSkipFor(note).propertyKey("topic").please();

    NoteRecallInfo noteRecallInfo = controller.getNoteInfo(note);

    assertThat(noteRecallInfo.getSkippedPropertyKeys(), contains("topic"));
  }

  @ParameterizedTest
  @CsvSource({"EASY, 4", "GOOD, 3", "HARD, 2", "AGAIN, 1"})
  void commissionedTrackerShowsMappedScoreFromLatestTutorLog(ProductOutcome outcome, int score)
      throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe
        .aRecallLogFor(makeMe.aMemoryTrackerFor(note).commissioned().please())
        .productOutcome(outcome)
        .please();

    assertThat(
        controller.getNoteInfo(note).getMemoryTrackers().getFirst().getLatestTutorFeedbackScore(),
        equalTo(score));
  }

  @Test
  void latestTutorLogWinsForTutorFeedbackScore() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    var tracker = makeMe.aMemoryTrackerFor(note).commissioned().please();
    makeMe
        .aRecallLogFor(tracker)
        .productOutcome(ProductOutcome.EASY)
        .recordedAt(makeMe.aTimestamp().of(1, 8).please())
        .please();
    makeMe
        .aRecallLogFor(tracker)
        .productOutcome(ProductOutcome.GOOD)
        .recordedAt(makeMe.aTimestamp().of(2, 8).please())
        .please();

    assertThat(
        controller.getNoteInfo(note).getMemoryTrackers().getFirst().getLatestTutorFeedbackScore(),
        equalTo(3));
  }
}
