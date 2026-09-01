package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.donut.controllers.dto.*;
import com.odde.donut.entities.*;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.MemoryTrackerService;
import com.odde.donut.services.RecallService;
import com.odde.donut.services.ResolvedWikiLinkService;
import com.odde.donut.services.UserService;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerDeleteTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired TextContentController textContentController;
  @Autowired EntityManager entityManager;
  @Autowired RecallService recallService;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired UserService userService;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
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
    Note target = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note unrelated = makeMe.aNote("Unrelated").underSameNotebookAs(target).please();
    Note referrer = makeMe.aNote("Referrer").underSameNotebookAs(target).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntarget: \"[[Target]]\"\n---\nBody [[Unrelated]]");
    textContentController.updateNoteContent(referrer, content);

    controller.deleteNote(target, removeFromPropertiesDeleteRequest());

    assertThat(referrer.getContent(), equalTo("---\ntype: Note\n---\nBody [[Unrelated]]"));
    assertThat(
        rowsFor(entityManager, referrer).stream()
            .map(AuthoredNoteReferenceRow::getAuthoredLink)
            .toList(),
        contains("Unrelated"));
    assertThat(
        controller.showNote(referrer).getWikiLinks().stream()
            .map(WikiLink::getDestinationNoteId)
            .toList(),
        contains(unrelated.getId()));
  }

  @Test
  void shouldReresolveNotebookShorthandsWhenDeleteRemovesACollision()
      throws UnexpectedNoAccessRightException {
    Note target1 = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target1.getNotebook()).name("Other Folder").please();
    Note target2 = makeMe.aNote("Target").folder(otherFolder).please();
    Note referrer =
        makeMe.aNote("Referrer").underSameNotebookAs(target1).content("See [[Target]].").please();
    resolvedWikiLinkService.refreshForNote(referrer, currentUser.getUser());
    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));

    controller.deleteNote(target2, leaveDeadLinksDeleteRequest());

    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
  }

  @Test
  void shouldReresolveNotebookShorthandsWhenRestoreReintroducesACollision()
      throws UnexpectedNoAccessRightException {
    Note target1 = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target1.getNotebook()).name("Other Folder").please();
    Note target2 = makeMe.aNote("Target").folder(otherFolder).please();
    Note referrer =
        makeMe.aNote("Referrer").underSameNotebookAs(target1).content("See [[Target]].").please();
    controller.deleteNote(target2, leaveDeadLinksDeleteRequest());
    resolvedWikiLinkService.refreshForNote(referrer, currentUser.getUser());
    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.undoDeleteNote(target2);

    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
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
      Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteNote(note, leaveDeadLinksDeleteRequest()));
    }

    @Test
    void shouldSoftDeleteNoteWhenDeleted() throws UnexpectedNoAccessRightException {
      controller.deleteNote(subject, leaveDeadLinksDeleteRequest());
      assertThat(subject.getDeletedAt(), is(not(nullValue())));
    }

    @Test
    void shouldNotCascadeSoftDeleteToStructuralChildNotes()
        throws UnexpectedNoAccessRightException {
      Note child = makeMe.aNote("child").underSameNotebookAs(subject).please();

      controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

      assertThat(subject.getDeletedAt(), is(not(nullValue())));
      assertThat(child.getDeletedAt(), nullValue());
    }

    @Nested
    class MemoryTrackerExclusionWhenNoteDeleted {
      private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

      private Note anotherNoteWithTracker() {
        Note other = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(other).please();
        return other;
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).please();
        anotherNoteWithTracker();
        testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        assertThat(recallService.getToRecallCount(currentUser.getUser(), currentTime, ZONE), is(1));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentLists()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).please();
        Note other = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
        MemoryTracker activeTracker = makeMe.aMemoryTrackerFor(other).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(
            memoryTrackerService.findLast100ByUser(currentUser.getUser().getId()),
            contains(activeTracker));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentlyRecalled()
          throws UnexpectedNoAccessRightException {
        MemoryTracker deletedTracker = makeMe.aMemoryTrackerFor(subject).please();
        anotherNoteWithTracker();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(
            memoryTrackerService.findLast100RecalledByUser(currentUser.getUser().getId()),
            not(hasItem(deletedTracker)));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromTotalAssimilatedCount()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).please();
        anotherNoteWithTracker();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        assertThat(
            recallService.getDueMemoryTrackers(currentUser.getUser(), currentTime, ZONE, 0)
                .totalAssimilatedCount,
            is(1));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromGetMemoryTrackersFor()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(0));
      }

      @Test
      void shouldRestoreMemoryTrackersWhenNoteIsRestored() throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());
        controller.undoDeleteNote(subject);

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(1));
      }
    }
  }
}
