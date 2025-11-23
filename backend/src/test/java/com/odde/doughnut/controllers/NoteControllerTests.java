package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.MemoryTrackerService;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.validation.Valid;
import java.io.IOException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindException;

class NoteControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NoteController controller;
  @Autowired RecallService recallService;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired UserService userService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class showNoteTest {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showNote(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe.aBazaarNotebook(note.getNotebook()).please();
      final NoteRealm noteRealm = controller.showNote(note);
      assertThat(noteRealm.getNote().getTopicConstructor(), equalTo(note.getTopicConstructor()));
      assertThat(noteRealm.getFromBazaar(), is(true));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      final NoteRealm noteRealm = controller.showNote(note);
      assertThat(noteRealm.getId(), equalTo(note.getId()));
      assertThat(noteRealm.getFromBazaar(), is(false));
    }
  }

  @Nested
  class showStatistics {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getNoteInfo(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe
          .aSubscription()
          .forUser(currentUser.getUser())
          .forNotebook(note.getNotebook())
          .please();
      makeMe.refresh(currentUser.getUser());
      assertThat(controller.getNoteInfo(note).getNote().getId(), equalTo(note.getId()));
    }
  }

  @Nested
  class updateNoteTest {
    Note note;
    NoteAccessoriesDTO noteAccessoriesDTO = new NoteAccessoriesDTO();

    @BeforeEach
    void setup() {
      note = makeMe.aNote("new").creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteAccessory response = controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(response.getNote().getId(), equalTo(note.getId()));
    }

    @Test
    void shouldAddUploadedImage() throws UnexpectedNoAccessRightException, IOException {
      noteAccessoriesDTO.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(note.getNoteAccessory().getImageAttachment(), is(not(nullValue())));
      note.getNoteAccessory().getImageAttachment().getBlob().getData();
    }

    @Test
    void shouldSaveTheBlogData() throws UnexpectedNoAccessRightException, IOException {
      noteAccessoriesDTO.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      byte[] data = note.getNoteAccessory().getImageAttachment().getBlob().getData();
      assertThat(data.length, is(68));
    }

    @Test
    void shouldNotRemoveTheImageIfNoNewImageInTheUpdate()
        throws UnexpectedNoAccessRightException, IOException {
      makeMe.theNote(note).withUploadedImage();
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(note.getNoteAccessory().getImageAttachment(), is(not(nullValue())));
    }
  }

  @Nested
  class DeleteNoteTest {
    Note subject;
    Note parent;
    Note child;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      subject = makeMe.aNote().under(parent).please();
      child = makeMe.aNote("child").under(subject).please();
    }

    @Test
    void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteNote(note));
    }

    @Test
    void shouldDeleteTheNoteButNotTheUser() throws UnexpectedNoAccessRightException {
      controller.deleteNote(subject);
      assertThat(parent.getChildren(), hasSize(0));
    }

    @Test
    void shouldDeleteTheChildNoteButNotSibling() throws UnexpectedNoAccessRightException {
      makeMe.aNote("silbling").under(parent).please();
      controller.deleteNote(subject);
      assertThat(parent.getChildren(), hasSize(1));
      assertThat(parent.getAllNoneLinkDescendants().toList(), hasSize(1));
    }

    @Nested
    class MemoryTrackerExclusionWhenNoteDeleted {
      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecallLists()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();
        testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());

        controller.deleteNote(subject);

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
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        MemoryTracker activeTracker =
            makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject);

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
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentlyReviewed()
          throws UnexpectedNoAccessRightException {
        MemoryTracker deletedTracker =
            makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        MemoryTracker activeTracker =
            makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject);

        assertThat(
            memoryTrackerService.findLast100ReviewedByUser(currentUser.getUser().getId()),
            not(hasItem(deletedTracker)));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromTotalAssimilatedCount()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject);

        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        RecallStatus status =
            recallService.getRecallStatus(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"));
        assertThat(status.totalAssimilatedCount, is(1));
      }

      @Test
      void shouldExcludeMemoryTrackersForDeletedNotesFromGetMemoryTrackersFor()
          throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();

        controller.deleteNote(subject);

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(0));
      }

      @Test
      void shouldRestoreMemoryTrackersWhenNoteIsRestored() throws UnexpectedNoAccessRightException {
        makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject);
        controller.undoDeleteNote(subject);

        assertThat(userService.getMemoryTrackersFor(currentUser.getUser(), subject), hasSize(1));
        Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
        RecallStatus status =
            recallService.getRecallStatus(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"));
        assertThat(status.totalAssimilatedCount, is(2));
      }
    }

    @Nested
    class UndoDeleteNoteTest {
      @Test
      void shouldUndoDeleteTheNote() throws UnexpectedNoAccessRightException {
        controller.deleteNote(subject);
        controller.undoDeleteNote(subject);
        assertThat(parent.getChildren(), hasSize(1));
        assertThat(parent.getAllNoneLinkDescendants().toList(), hasSize(2));
      }

      @Test
      void shouldUndoOnlyLastChange() throws UnexpectedNoAccessRightException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(child);

        timestamp = TimestampOperations.addHoursToTimestamp(timestamp, 1);
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(subject);

        controller.undoDeleteNote(subject);
        assertThat(parent.getAllNoneLinkDescendants().toList(), hasSize(1));
      }
    }
  }

  @Nested
  class UpdateWikidataId {
    Note note;
    Note parent;
    String noteWikidataId = "Q1234";

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note = makeMe.aNote().under(parent).please();
    }

    @Test
    void shouldUpdateNoteWithUniqueWikidataId()
        throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = "Q123";
      controller.updateWikidataId(note, wikidataAssociationCreation);
      Note sameNote = noteRepository.findById(note.getId()).get();
      assertThat(sameNote.getWikidataId(), equalTo("Q123"));
    }

    @Test
    void shouldNotUpdateWikidataIdIfParentNoteSameWikidataId() {
      makeMe.aNote().under(parent).wikidataId(noteWikidataId).please();

      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = noteWikidataId;
      BindException bindException =
          assertThrows(
              BindException.class,
              () -> controller.updateWikidataId(note, wikidataAssociationCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }
  }

  @Nested
  class UpdateRecallSetting {
    Note source;
    Note target;
    Note link;

    @BeforeEach
    void setup() {
      source = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      target = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      link = makeMe.aReification().between(source, target).please();
    }

    @Test
    void shouldUpdateLinkLevel() throws UnexpectedNoAccessRightException {
      @Valid RecallSetting recallSetting = new RecallSetting();
      recallSetting.setLevel(4);
      controller.updateRecallSetting(source, recallSetting);
      assertThat(getLevel(link), is(4));
    }

    @Test
    void shouldUpdateReferenceLevel() throws UnexpectedNoAccessRightException {
      @Valid RecallSetting recallSetting = new RecallSetting();
      recallSetting.setLevel(4);
      controller.updateRecallSetting(target, recallSetting);
      assertThat(getLevel(link), is(4));
    }

    private static Integer getLevel(Note link) {
      return link.getRecallSetting().getLevel();
    }
  }

  @Nested
  class GraphTests {
    Note rootNote;
    Note child1;

    @BeforeEach
    void setup() {
      rootNote = makeMe.aNote("Root").creatorAndOwner(currentUser.getUser()).please();
      child1 = makeMe.aNote("Child 1").under(rootNote).please();
      makeMe.refresh(rootNote);
    }

    @Test
    void shouldReturnGraphWithDefaultTokenLimit() throws UnexpectedNoAccessRightException {
      GraphRAGResult result = controller.getGraph(rootNote, 5000);

      assertThat(result.getFocusNote().getUri(), equalTo(rootNote.getUri()));
      assertThat(result.getRelatedNotes(), is(not(empty())));
    }

    @Test
    void shouldRespectCustomTokenLimit() throws UnexpectedNoAccessRightException {
      GraphRAGResult result = controller.getGraph(rootNote, 1);
      assertThat(result.getRelatedNotes(), is(empty()));
    }

    @Test
    void shouldNotAllowAccessToUnauthorizedNotes() {
      User otherUser = makeMe.aUser().please();
      Note unauthorizedNote = makeMe.aNote().creatorAndOwner(otherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getGraph(unauthorizedNote, 5000));
    }

    @Test
    void shouldReturnAllDescendants() throws UnexpectedNoAccessRightException {
      Note grandchild = makeMe.aNote("Grandchild").under(child1).please();
      makeMe.refresh(rootNote);
      GraphRAGResult result = controller.getDescendants(rootNote);
      assertThat(result.getFocusNote().getUri(), equalTo(rootNote.getUri()));
      assertThat(result.getRelatedNotes(), hasSize(2));
      assertThat(
          result.getRelatedNotes().stream().map(n -> n.getTitle()).toList(),
          containsInAnyOrder("Child 1", "Grandchild"));
    }

    @Test
    void shouldThrowWhenUserDoesNotOwnTheNote() {
      User otherUser = makeMe.aUser().please();
      Note otherUsersNote = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getDescendants(otherUsersNote));
    }
  }
}
