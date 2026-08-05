package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerDeleteTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired RecallService recallService;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired UserService userService;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private NoteDeleteDTO leaveDeadLinksDeleteRequest() {
    NoteDeleteDTO dto = new NoteDeleteDTO();
    dto.setReferenceHandling(NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS);
    return dto;
  }

  private NoteDeleteDTO removeFromPropertiesDeleteRequest() {
    NoteDeleteDTO dto = new NoteDeleteDTO();
    dto.setReferenceHandling(NoteDeleteReferenceHandling.REMOVE_FROM_PROPERTIES);
    return dto;
  }

  @Test
  void shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly()
      throws UnexpectedNoAccessRightException {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note target = makeMe.aNote("Target").notebook(nb).please();
    Note referrer =
        makeMe
            .aNote("Referrer")
            .notebook(nb)
            .content("---\nsource: \"[[Referrer]]\"\ntarget: \"[[Target]]\"\n---\nBody [[Target]]")
            .please();
    wikiTitleCacheService.refreshForNote(referrer, currentUser.getUser());

    controller.deleteNote(target, removeFromPropertiesDeleteRequest());

    assertThat(referrer.getContent(), equalTo("---\nsource: '[[Referrer]]'\n---\nBody [[Target]]"));
  }

  @Nested
  class SoftDeleteNote {
    Note subject;

    @BeforeEach
    void setup() {
      subject = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    }

    @Test
    void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteNote(note, leaveDeadLinksDeleteRequest()));
    }

    @Test
    void shouldSoftDeleteNoteWhenDeleted() throws UnexpectedNoAccessRightException {
      controller.deleteNote(subject, leaveDeadLinksDeleteRequest());
      assertThat(subject.getDeletedAt(), is(not(nullValue())));
    }

    @Nested
    class MemoryTrackerExclusionWhenNoteDeleted {
      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();
        testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        int toRecallCount =
            recallService.getToRecallCount(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"));
        assertThat(toRecallCount, is(1));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentLists()
          throws UnexpectedNoAccessRightException {
        MemoryTracker deletedTracker =
            makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        MemoryTracker activeTracker =
            makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(
            memoryTrackerService.findLast100ByUser(currentUser.getUser().getId()), hasSize(1));
        assertThat(
            memoryTrackerService.findLast100ByUser(currentUser.getUser().getId()),
            contains(activeTracker));
        assertThat(
            memoryTrackerService.findLast100ByUser(currentUser.getUser().getId()),
            not(hasItem(deletedTracker)));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentlyRecalled()
          throws UnexpectedNoAccessRightException {
        MemoryTracker deletedTracker =
            makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(
            memoryTrackerService.findLast100RecalledByUser(currentUser.getUser().getId()),
            not(hasItem(deletedTracker)));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromTotalAssimilatedCount()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        var status =
            recallService.getDueMemoryTrackers(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"), 0);
        assertThat(status.totalAssimilatedCount, is(1));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromGetMemoryTrackersFor()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(0));
      }

      @Test
      void shouldRestoreMemoryTrackersWhenNoteIsRestored() throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());
        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(0));
        controller.undoDeleteNote(subject);

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(1));
      }
    }
  }
}
