package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.ImageRepository;
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
import jakarta.validation.Validation;
import java.io.IOException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired ImageRepository imageRepository;
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
      assertThat(noteRealm.getNotebookRealm().readonly(), is(true));
      assertThat(
          noteRealm.getNotebookRealm().notebook().getId(), equalTo(note.getNotebook().getId()));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      final NoteRealm noteRealm = controller.showNote(note);
      assertThat(noteRealm.getId(), equalTo(note.getId()));
      assertThat(noteRealm.getNotebookRealm().readonly(), is(false));
      assertThat(
          noteRealm.getNotebookRealm().notebook().getId(), equalTo(note.getNotebook().getId()));
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
              .content("Text [[LinkedPage]] and [[NoSuch]].")
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
              .content("See [[Other Notebook:LinkedPage]] for more.")
              .please();
      wikiTitleCacheService.refreshForNote(viewer, user);
      NoteRealm realm = controller.showNote(viewer);
      assertThat(realm.getWikiTitles(), hasSize(1));
      assertThat(realm.getWikiTitles().get(0).getLinkText(), equalTo("Other Notebook:LinkedPage"));
      assertThat(realm.getWikiTitles().get(0).getNoteId(), equalTo(targetInOther.getId()));
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
      String markdown =
          "---\n"
              + "see: Summary with [[FrontmatterTarget]]\n"
              + "---\n"
              + "[[FrontmatterTarget]] body\n";
      Note viewer =
          makeMe.aNote().creator(user).inNotebook(root.getNotebook()).content(markdown).please();
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
  class UploadNoteImage {
    @Test
    void shouldReturnImagePathAndPersistImageLinkedToNote()
        throws UnexpectedNoAccessRightException, IOException {
      Note note = makeMe.aNote("n").creatorAndOwner(currentUser.getUser()).please();
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());

      NoteImageUploadResult result = controller.uploadNoteImage(note, dto);

      assertThat(result.imagePath(), startsWith("/attachments/images/"));
      String[] segments = result.imagePath().split("/");
      assertThat(segments.length, equalTo(5));
      assertThat(segments[1], equalTo("attachments"));
      assertThat(segments[2], equalTo("images"));
      int imageId = Integer.parseInt(segments[3]);
      assertThat(segments[4], equalTo("my.png"));
      Image saved = imageRepository.findById(imageId).orElseThrow();
      assertThat(saved.getNote().getId(), equalTo(note.getId()));
    }

    @Test
    void shouldNotAllowUploadForNoteBelongingToAnotherUser() {
      User other = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(other).please();
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.uploadNoteImage(note, dto));
    }

    @Test
    void shouldRejectInvalidUploadContentType() {
      try (var factory = Validation.buildDefaultValidatorFactory()) {
        NoteImageUploadDTO dto = new NoteImageUploadDTO();
        dto.setUploadImage(
            new MockMultipartFile(
                "uploadImage", "x.pdf", "application/pdf", "not-empty".getBytes()));
        assertThat(factory.getValidator().validate(dto), is(not(empty())));
      }
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
    void shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly()
        throws UnexpectedNoAccessRightException {
      Note target = makeMe.aNote("Target").creatorAndOwner(currentUser.getUser()).please();
      Note referrer =
          makeMe
              .aNote("Referrer")
              .creator(currentUser.getUser())
              .underSameNotebookAs(target)
              .content(
                  "---\nsource: \"[[Referrer]]\"\ntarget: \"[[Target]]\"\n---\nBody [[Target]]")
              .please();
      wikiTitleCacheService.refreshForNote(referrer, currentUser.getUser());

      controller.deleteNote(target, removeFromPropertiesDeleteRequest());

      assertThat(
          referrer.getContent(), equalTo("---\nsource: '[[Referrer]]'\n---\nBody [[Target]]"));
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
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
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
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        MemoryTracker activeTracker =
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
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
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
        Note otherNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
        makeMe.aMemoryTrackerFor(otherNote).by(currentUser.getUser()).please();

        controller.deleteNote(subject, leaveDeadLinksDeleteRequest());
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
  class UpdateNoteRecallSetting {
    Note source;
    Note target;

    @BeforeEach
    void setup() {
      source = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      target = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
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

  @Nested
  class AiContextMarkdownTests {
    @Test
    void shouldReturnMarkdownForReadableNote() throws UnexpectedNoAccessRightException {
      Note note =
          makeMe.aNote("Focus").content("Body").creatorAndOwner(currentUser.getUser()).please();
      NoteAiContextMarkdown dto = controller.getAiContextMarkdown(note, 5000);
      assertThat(dto.markdown(), containsString("Focus"));
      assertThat(dto.markdown(), containsString("Body"));
    }

    @Test
    void shouldNotAllowAccessToUnauthorizedNotes() {
      User otherUser = makeMe.aUser().please();
      Note unauthorizedNote = makeMe.aNote().creatorAndOwner(otherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAiContextMarkdown(unauthorizedNote, 5000));
    }
  }
}
