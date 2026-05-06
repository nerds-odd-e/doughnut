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
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.RecallService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
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
  @Autowired NoteService noteService;
  @Autowired RecallService recallService;
  @Autowired MemoryTrackerService memoryTrackerService;
  @Autowired UserService userService;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
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
      assertThat(noteRealm.getNote().getTitle(), equalTo(note.getTitle()));
      assertThat(noteRealm.getFromBazaar(), is(true));
      assertThat(noteRealm.getNotebookId(), equalTo(note.getNotebook().getId()));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      final NoteRealm noteRealm = controller.showNote(note);
      assertThat(noteRealm.getId(), equalTo(note.getId()));
      assertThat(noteRealm.getFromBazaar(), is(false));
      assertThat(noteRealm.getNotebookId(), equalTo(note.getNotebook().getId()));
    }

    @Test
    void shouldReturnWikiTitlesForUnqualifiedLinksByNotebookNameAndTitle()
        throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Note root = makeMe.aNote().creatorAndOwner(user).title("root-head").please();
      Note matched =
          makeMe.aNote().title("LinkedPage").creator(user).inNotebook(root.getNotebook()).please();
      Note viewer =
          makeMe
              .aNote()
              .creator(user)
              .inNotebook(root.getNotebook())
              .details("Text [[LinkedPage]] and [[NoSuch]].")
              .please();
      wikiTitleCacheService.refreshForNote(viewer, user);
      NoteRealm realm = controller.showNote(viewer);
      assertThat(realm.getWikiTitles(), hasSize(1));
      assertThat(realm.getWikiTitles().get(0).getLinkText(), equalTo("LinkedPage"));
      assertThat(realm.getWikiTitles().get(0).getNoteId(), equalTo(matched.getId()));
    }

    @Test
    void shouldReturnWikiTitlesForQualifiedLinkToNoteInAnotherNotebook()
        throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Notebook otherNotebook =
          makeMe.aNotebook().creatorAndOwner(user).name("Other Notebook").please();
      makeMe.aNote().creator(user).inNotebook(otherNotebook).please();
      Note targetInOther =
          makeMe.aNote().title("LinkedPage").creator(user).inNotebook(otherNotebook).please();
      Notebook mainNotebook = makeMe.aNotebook().creatorAndOwner(user).name("Main").please();
      makeMe.aNote().creator(user).inNotebook(mainNotebook).please();
      Note viewer =
          makeMe
              .aNote()
              .creator(user)
              .inNotebook(mainNotebook)
              .details("See [[Other Notebook:LinkedPage]] for more.")
              .please();
      wikiTitleCacheService.refreshForNote(viewer, user);
      NoteRealm realm = controller.showNote(viewer);
      assertThat(realm.getWikiTitles(), hasSize(1));
      assertThat(realm.getWikiTitles().get(0).getLinkText(), equalTo("Other Notebook:LinkedPage"));
      assertThat(realm.getWikiTitles().get(0).getNoteId(), equalTo(targetInOther.getId()));
    }

    @Test
    void shouldOmitQualifiedWikiLinkWhenTargetNotebookIsNotReadable()
        throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Notebook secretNotebook =
          makeMe.aNotebook().creatorAndOwner(otherUser).name("Secret Notebook").please();
      makeMe.aNote().creator(otherUser).inNotebook(secretNotebook).please();
      makeMe.aNote().title("Hidden Note").creator(otherUser).inNotebook(secretNotebook).please();

      User viewerUser = currentUser.getUser();
      Notebook myNotebook =
          makeMe.aNotebook().creatorAndOwner(viewerUser).name("My Notebook").please();
      makeMe.aNote().creator(viewerUser).inNotebook(myNotebook).please();
      Note viewer =
          makeMe
              .aNote()
              .creator(viewerUser)
              .inNotebook(myNotebook)
              .details("Try [[Secret Notebook:Hidden Note]].")
              .please();
      NoteRealm realm = controller.showNote(viewer);
      assertThat(realm.getWikiTitles(), empty());
    }

    @Test
    void shouldReturnWikiTitlesFromFrontmatterBlocks() throws UnexpectedNoAccessRightException {
      User user = currentUser.getUser();
      Note root = makeMe.aNote().creatorAndOwner(user).please();
      Note fromFm =
          makeMe
              .aNote()
              .title("FrontmatterTarget")
              .creator(user)
              .inNotebook(root.getNotebook())
              .please();
      String details =
          "---\n"
              + "see: Summary with [[FrontmatterTarget]]\n"
              + "---\n"
              + "[[FrontmatterTarget]] body\n";
      Note viewer =
          makeMe.aNote().creator(user).inNotebook(root.getNotebook()).details(details).please();
      wikiTitleCacheService.refreshForNote(viewer, user);
      NoteRealm realm = controller.showNote(viewer);
      assertThat(realm.getWikiTitles(), hasSize(1));
      assertThat(realm.getWikiTitles().get(0).getLinkText(), equalTo("FrontmatterTarget"));
      assertThat(realm.getWikiTitles().get(0).getNoteId(), equalTo(fromFm.getId()));
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
      assertThat(controller.getNoteInfo(note), notNullValue());
    }

    @Test
    void shouldIncludeSkippedMemoryTrackersInNoteInfo() throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe
          .aSubscription()
          .forUser(currentUser.getUser())
          .forNotebook(note.getNotebook())
          .please();
      makeMe.refresh(currentUser.getUser());

      makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      makeMe
          .aMemoryTrackerFor(note)
          .by(currentUser.getUser())
          .spelling()
          .removedFromTracking()
          .please();

      NoteRecallInfo noteRecallInfo = controller.getNoteInfo(note);
      assertThat(noteRecallInfo.getMemoryTrackers(), hasSize(2));
      assertThat(
          noteRecallInfo.getMemoryTrackers().stream()
              .anyMatch(mt -> Boolean.TRUE.equals(mt.getRemovedFromTracking())),
          is(true));
    }
  }

  @Nested
  class VerifySpelling {
    @Test
    void returnsCorrectWhenSpellingMatches() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().title("sedition").creatorAndOwner(currentUser.getUser()).please();
      AnswerSpellingDTO dto = new AnswerSpellingDTO();
      dto.setSpellingAnswer("sedition");

      SpellingVerificationResult result = controller.verifySpelling(note, dto);

      assertThat(result.correct(), is(true));
    }

    @Test
    void returnsCorrectWhenAlternativeSpellingMatches() throws UnexpectedNoAccessRightException {
      Note note =
          makeMe.aNote().title("colour／color").creatorAndOwner(currentUser.getUser()).please();
      AnswerSpellingDTO dto = new AnswerSpellingDTO();
      dto.setSpellingAnswer("colour");

      SpellingVerificationResult result = controller.verifySpelling(note, dto);

      assertThat(result.correct(), is(true));
    }

    @Test
    void returnsIncorrectWhenSpellingDoesNotMatch() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().title("sedition").creatorAndOwner(currentUser.getUser()).please();
      AnswerSpellingDTO dto = new AnswerSpellingDTO();
      dto.setSpellingAnswer("wrong answer");

      SpellingVerificationResult result = controller.verifySpelling(note, dto);

      assertThat(result.correct(), is(false));
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
    Note child;

    @BeforeEach
    void setup() {
      subject = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      child =
          makeMe
              .aNote("child")
              .creator(currentUser.getUser())
              .inNotebook(subject.getNotebook())
              .please();
    }

    @Test
    void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteNote(note));
    }

    @Test
    void shouldSoftDeleteNoteWhenDeleted() throws UnexpectedNoAccessRightException {
      controller.deleteNote(subject);
      assertThat(subject.getDeletedAt(), is(not(nullValue())));
    }

    @Test
    void shouldNotSoftDeleteOutgoingRelationshipNotesWhenSubjectIsDeleted()
        throws UnexpectedNoAccessRightException {
      Note targetNote = makeMe.aNote("target").creatorAndOwner(currentUser.getUser()).please();
      Note relation = makeMe.aRelation().between(subject, targetNote).please();
      makeMe.refresh(subject);

      controller.deleteNote(subject);

      assertThat(subject.getDeletedAt(), is(not(nullValue())));
      assertThat(relation.getDeletedAt(), is(nullValue()));
    }

    @Test
    void shouldNotSoftDeleteInboundRelationshipNotesWhenFocalNoteIsDeleted()
        throws UnexpectedNoAccessRightException {
      Note sourceNote = makeMe.aNote("source").creatorAndOwner(currentUser.getUser()).please();
      Note reference = makeMe.aRelation().between(sourceNote, subject).please();
      makeMe.refresh(subject);

      controller.deleteNote(subject);

      assertThat(subject.getDeletedAt(), is(not(nullValue())));
      assertThat(reference.getDeletedAt(), is(nullValue()));
    }

    @Test
    void shouldNotSoftDeleteInboundReferencesToStructuralChildrenWhenParentNoteIsDeleted()
        throws UnexpectedNoAccessRightException {
      Note targetNote = makeMe.aNote("target").creatorAndOwner(currentUser.getUser()).please();
      Note referenceToChild = makeMe.aRelation().between(targetNote, child).please();
      makeMe.refresh(subject);

      controller.deleteNote(subject);

      assertThat(subject.getDeletedAt(), is(not(nullValue())));
      assertThat(child.getDeletedAt(), is(nullValue()));
      assertThat(referenceToChild.getDeletedAt(), is(nullValue()));
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
      void shouldExcludeMemoryTrackersForDeletedNotesFromRecentlyRecalled()
          throws UnexpectedNoAccessRightException {
        MemoryTracker deletedTracker =
            makeMe.aMemoryTrackerFor(subject).by(currentUser.getUser()).please();
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        MemoryTracker activeTracker =
            makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject);

        assertThat(
            memoryTrackerService.findLast100RecalledByUser(currentUser.getUser().getId()),
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
        var status =
            recallService.getDueMemoryTrackers(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"), 0);
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
        var status =
            recallService.getDueMemoryTrackers(
                currentUser.getUser(), currentTime, java.time.ZoneId.of("Asia/Shanghai"), 0);
        assertThat(status.totalAssimilatedCount, is(2));
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
      note =
          makeMe.aNote().creator(currentUser.getUser()).inNotebook(parent.getNotebook()).please();
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
      makeMe
          .aNote()
          .creator(currentUser.getUser())
          .inNotebook(parent.getNotebook())
          .wikidataId(noteWikidataId)
          .please();

      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = noteWikidataId;
      BindException bindException =
          assertThrows(
              BindException.class,
              () -> controller.updateWikidataId(note, wikidataAssociationCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }

    @Test
    void shouldClearWikidataIdWhenEmptyStringProvided()
        throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
      note =
          makeMe
              .aNote()
              .creator(currentUser.getUser())
              .inNotebook(parent.getNotebook())
              .wikidataId("Q123")
              .please();

      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = "";
      controller.updateWikidataId(note, wikidataAssociationCreation);
      Note sameNote = noteRepository.findById(note.getId()).get();
      assertThat(sameNote.getWikidataId(), equalTo(null));
    }
  }

  @Nested
  class UpdateNoteRecallSetting {
    Note source;
    Note target;
    Note relation;

    @BeforeEach
    void setup() {
      source = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      target = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      relation = makeMe.aRelation().between(source, target).please();
    }

    @Test
    void shouldNotChangeRelationshipNoteRecallLevelWhenUpdatingSource()
        throws UnexpectedNoAccessRightException {
      int relationLevelBefore = getLevel(relation);
      @Valid NoteRecallSetting noteRecallSetting = new NoteRecallSetting();
      noteRecallSetting.setLevel(4);
      controller.updateNoteRecallSetting(source, noteRecallSetting);
      assertThat(
          getLevel(noteRepository.findById(relation.getId()).orElseThrow()),
          is(relationLevelBefore));
      assertThat(
          noteRepository.findById(source.getId()).orElseThrow().getRecallSetting().getLevel(),
          is(4));
    }

    @Test
    void shouldNotChangeRelationshipNoteRecallLevelWhenUpdatingTarget()
        throws UnexpectedNoAccessRightException {
      int relationLevelBefore = getLevel(relation);
      @Valid NoteRecallSetting noteRecallSetting = new NoteRecallSetting();
      noteRecallSetting.setLevel(4);
      controller.updateNoteRecallSetting(target, noteRecallSetting);
      assertThat(
          getLevel(noteRepository.findById(relation.getId()).orElseThrow()),
          is(relationLevelBefore));
      assertThat(
          noteRepository.findById(target.getId()).orElseThrow().getRecallSetting().getLevel(),
          is(4));
    }

    @Test
    void shouldPutNoteBackToAssimilationListWhenRememberSpellingIsAddedLater()
        throws UnexpectedNoAccessRightException {
      makeMe.aMemoryTrackerFor(source).by(currentUser.getUser()).please();
      assertThat(
          userService.getUnassimilatedNotes(currentUser.getUser()).toList(),
          not(hasItem(hasProperty("id", equalTo(source.getId())))));

      NoteRecallSetting noteRecallSetting = new NoteRecallSetting();
      noteRecallSetting.setRememberSpelling(true);
      controller.updateNoteRecallSetting(source, noteRecallSetting);

      assertThat(
          userService.getUnassimilatedNotes(currentUser.getUser()).toList(),
          hasItem(hasProperty("id", equalTo(source.getId()))));
    }

    private static Integer getLevel(Note relation) {
      return relation.getRecallSetting().getLevel();
    }
  }

  @Nested
  class GraphTests {
    Note rootNote;

    @BeforeEach
    void setup() {
      rootNote = makeMe.aNote("Root").creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnGraphWithDefaultTokenLimit() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 5000);

      assertThat(result.getFocusNote().getNotebook(), equalTo(rootNote.getNotebook().getName()));
      assertThat(result.getFocusNote().getDepth(), equalTo(0));
      assertThat(result.getFocusNote().getOutgoingLinks(), is(notNullValue()));
    }

    @Test
    void shouldRespectCustomTokenLimit() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 1);
      assertThat(result.getRelatedNotes(), is(empty()));
    }

    @Test
    void relatedNotesExposeEdgeTypeDepthAndPath() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 5000);
      for (var n : result.getRelatedNotes()) {
        assertThat(n.getEdgeType(), is(notNullValue()));
        assertThat(n.getDepth(), greaterThan(0));
        assertThat(n.getRetrievalPath(), is(notNullValue()));
      }
    }

    @Test
    void shouldNotAllowAccessToUnauthorizedNotes() {
      User otherUser = makeMe.aUser().please();
      Note unauthorizedNote = makeMe.aNote().creatorAndOwner(otherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getGraph(unauthorizedNote, 5000));
    }
  }
}
