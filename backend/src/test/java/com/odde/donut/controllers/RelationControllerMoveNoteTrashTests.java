package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.SearchTerm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.RelationshipLiteralSearchHits;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerMoveNoteTrashTests extends ControllerTestBase {
  @Autowired NoteController noteController;
  @Autowired RelationController controller;
  @Autowired SearchController searchController;
  @Autowired RecallsController recallsController;
  @Autowired AssimilationController assimilationController;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
  }

  @Test
  void movingLearnedNoteIntoAndOutOfTrashChangesParticipationButPreservesItsData()
      throws UnexpectedNoAccessRightException {
    Timestamp now = makeMe.aTimestamp().of(1, 8).please();
    testabilitySettings.timeTravelTo(now);
    Notebook notebook = ownedNotebook("Knowledge");
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder descendant = makeMe.aFolder().parentFolder(trash).name("Old topics").please();
    Note target = makeMe.aNote("Target topic").notebook(notebook).please();
    Note referrer = makeMe.aNote("Referrer").notebook(notebook).please();
    authorReferencingContent(referrer, "See [[Target topic]].");
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(target).assimilatedAt(now).recallCount(1).please();
    MemoryTracker removedTracker =
        makeMe.aMemoryTrackerFor(target).spelling().removedFromTracking().please();

    assertParticipation(target, referrer, tracker, true);
    assertThat(
        searchController.searchForRelationshipTarget(searchTerm("Old topics")).stream()
            .noneMatch(
                hit -> hit.getFolderId() != null && hit.getFolderId().equals(descendant.getId())),
        equalTo(true));

    controller.moveNoteToFolder(target, descendant);

    assertThat(target.isAvailable(), equalTo(false));
    assertParticipation(target, referrer, tracker, false);
    assertThat(
        memoryTrackerController.showMemoryTracker(removedTracker).getRemovedFromTracking(),
        equalTo(true));
    assertThat(memoryTrackerController.getRecallHistory(tracker), hasSize(1));

    controller.moveNoteToNotebookRoot(target);

    assertThat(target.isAvailable(), equalTo(true));
    assertParticipation(target, referrer, tracker, true);
  }

  @Test
  void webTrashThenOrdinaryMovePreservesNoteAndTrackerIdsHistoryAndPreferences()
      throws UnexpectedNoAccessRightException {
    Timestamp now = makeMe.aTimestamp().of(1, 8).please();
    testabilitySettings.timeTravelTo(now);
    Notebook notebook = ownedNotebook("Knowledge");
    Folder activeFolder = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note target = makeMe.aNote("Cells").folder(activeFolder).please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(target).assimilatedAt(now).recallCount(1).please();
    MemoryTracker removedTracker =
        makeMe.aMemoryTrackerFor(target).spelling().removedFromTracking().please();
    Integer noteId = target.getId();
    Integer trackerId = tracker.getId();
    Integer removedTrackerId = removedTracker.getId();

    noteController.trashNote(target, leaveDeadLinks());

    assertThat(target.getId(), equalTo(noteId));
    assertThat(target.isTrashed(), equalTo(true));
    assertThat(target.getFolder().getParentFolder().getName(), equalTo("_trash"));

    controller.moveNoteToFolder(target, activeFolder);

    makeMe.refresh(target);
    assertThat(target.getId(), equalTo(noteId));
    assertThat(target.isTrashed(), equalTo(false));
    assertThat(target.getFolder().getId(), equalTo(activeFolder.getId()));
    assertThat(
        memoryTrackerRepository.findById(trackerId).orElseThrow().getNote().getId(),
        equalTo(noteId));
    assertThat(memoryTrackerController.getRecallHistory(tracker), hasSize(1));
    assertThat(
        memoryTrackerRepository.findById(removedTrackerId).orElseThrow().getRemovedFromTracking(),
        equalTo(true));
  }

  private void assertParticipation(
      Note target, Note referrer, MemoryTracker tracker, boolean available)
      throws UnexpectedNoAccessRightException {
    assertThat(
        RelationshipLiteralSearchHits.noteMatches(
                searchController.searchForRelationshipTarget(searchTerm(target.getTitle())))
            .stream()
            .anyMatch(result -> result.getNoteTopology().getId() == target.getId()),
        equalTo(available));
    assertThat(
        recallsController.recalling("Asia/Shanghai", 0).getToRepeat().stream()
            .anyMatch(candidate -> candidate.getMemoryTrackerId() == tracker.getId()),
        equalTo(available));
    assertThat(
        assimilationController.next("Asia/Shanghai").getCounts().getAssimilatedCountOfTheDay(),
        equalTo(available ? 1 : 0));
    List<WikiLink> wikiLinks = noteController.showNote(referrer).getWikiLinks();
    assertThat(wikiLinks, hasSize(available ? 1 : 0));
    if (available) {
      assertThat(wikiLinks.getFirst().getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    }
    assertThat(noteController.showNote(target).getNote().isTrashed(), equalTo(!available));
  }

  private SearchTerm searchTerm(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    return searchTerm;
  }

  @Test
  void moveFromTrashIntoOccupiedDestinationReportsOrdinaryConflictAndLeavesBothNotesIntact()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook("Knowledge");
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note trashed = makeMe.aNote("Cells").folder(biology).please();
    noteController.trashNote(trashed, leaveDeadLinks());
    makeMe.refresh(trashed);
    Folder trashParent = trashed.getFolder();
    assertThat(trashed.isTrashed(), equalTo(true));
    Note occupier = makeMe.aNote("Cells").folder(biology).please();
    Integer occupierId = occupier.getId();

    ApiException conflict =
        assertThrows(ApiException.class, () -> controller.moveNoteToFolder(trashed, biology));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    makeMe.refresh(trashed);
    makeMe.refresh(occupier);
    assertThat(trashed.isTrashed(), equalTo(true));
    assertThat(trashed.getFolder().getId(), equalTo(trashParent.getId()));
    assertThat(occupier.getId(), equalTo(occupierId));
    assertThat(occupier.getFolder().getId(), equalTo(biology.getId()));
    assertThat(occupier.getTitle(), equalTo("Cells"));

    controller.moveNoteToNotebookRoot(trashed);

    makeMe.refresh(trashed);
    assertThat(trashed.isTrashed(), equalTo(false));
    assertThat(trashed.getFolder(), nullValue());
    assertThat(occupier.getFolder().getId(), equalTo(biology.getId()));
  }
}
