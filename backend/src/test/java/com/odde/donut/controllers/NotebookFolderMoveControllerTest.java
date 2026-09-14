package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderMoveControllerTest extends NotebookFolderManagementControllerTestBase {

  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void movesRetainedTrashedSubtreeBackToActiveParticipation()
      throws UnexpectedNoAccessRightException {
    Timestamp now = makeMe.aTimestamp().of(1, 8).please();
    testabilitySettings.timeTravelTo(now);
    Notebook notebook = ownedNotebook();
    Folder trash = ownedFolder(notebook, "_trash");
    Folder retained =
        makeMe.aFolder().parentFolder(trash).name("Biology").readmeContent("# Biology").please();
    Folder emptyDescendant = makeMe.aFolder().parentFolder(retained).name("Empty").please();
    Note directNote = makeMe.aNote("Cells").folder(retained).please();
    Folder nested = makeMe.aFolder().parentFolder(retained).name("Nested").please();
    Note learnedNote = makeMe.aNote("Genetics").folder(nested).please();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(learnedNote).assimilatedAt(now).recallCount(1).please();
    MemoryTracker stoppedTracker =
        makeMe.aMemoryTrackerFor(learnedNote).spelling().removedFromTracking().please();

    assertThat(retained.isTrashed(), equalTo(true));
    assertThat(directNote.isAvailable(), equalTo(false));
    assertThat(tracker.isActive(), equalTo(false));
    Integer retainedId = retained.getId();
    Integer directNoteId = directNote.getId();
    Integer learnedNoteId = learnedNote.getId();
    Integer trackerId = tracker.getId();
    Integer stoppedTrackerId = stoppedTracker.getId();

    controller.moveFolder(notebook, retained, folderMove(null));

    makeMe.refresh(retained);
    makeMe.refresh(emptyDescendant);
    makeMe.refresh(directNote);
    makeMe.refresh(learnedNote);
    MemoryTracker recoveredTracker = memoryTrackerRepository.findById(trackerId).orElseThrow();
    assertThat(retained.getId(), equalTo(retainedId));
    assertThat(retained.getParentFolder(), nullValue());
    assertThat(retained.getReadmeContent(), equalTo("# Biology"));
    assertThat(retained.isTrashed(), equalTo(false));
    assertThat(emptyDescendant.getParentFolder().getId(), equalTo(retainedId));
    assertThat(directNote.getId(), equalTo(directNoteId));
    assertThat(directNote.isAvailable(), equalTo(true));
    assertThat(learnedNote.getId(), equalTo(learnedNoteId));
    assertThat(learnedNote.isAvailable(), equalTo(true));
    assertThat(recoveredTracker.getNote().getId(), equalTo(learnedNoteId));
    assertThat(recoveredTracker.isActive(), equalTo(true));
    assertThat(memoryTrackerController.getRecallHistory(tracker), hasSize(1));
    assertThat(
        memoryTrackerRepository.findById(stoppedTrackerId).orElseThrow().getRemovedFromTracking(),
        equalTo(true));
  }

  @Test
  void movesChildFolderToNotebookRoot() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder parent = ownedFolder(nb, "Parent");
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

    Folder result = controller.moveFolder(nb, child, folderMove(null));

    assertThat(result.getName(), equalTo("Child"));
    assertTrue(listingHasFolder(nb, null, child));
    assertThat(listingHasFolder(nb, parent.getId(), child), equalTo(false));
  }

  @Test
  void rejectsMoveIntoDescendant() {
    Notebook nb = ownedNotebook();
    Folder outer = ownedFolder(nb, "Outer");
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(nb, outer, folderMove(inner.getId())));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Cannot move folder into its descendant."));
  }

  @Test
  void rejectsSelfAsDestination() {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "Solo");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(nb, folder, folderMove(folder.getId())));
    assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
  }

  @Test
  void rejectsDuplicateNameAtDestination() {
    Notebook nb = ownedNotebook();
    ownedFolder(nb, "Dup");
    Folder holder = ownedFolder(nb, "Holder");
    Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> controller.moveFolder(nb, nestedDup, folderMove(null)));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
  }

  @Test
  void moveConflictLeavesTrashedSubtreeRecoverable() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook();
    Folder trash = ownedFolder(notebook, "_trash");
    Folder trashed = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    Note retained = makeMe.aNote("Retained").folder(trashed).please();
    Folder occupiedDestination = ownedFolder(notebook, "Occupied destination");
    makeMe.aFolder().parentFolder(occupiedDestination).name("Biology").please();
    Folder freeDestination = ownedFolder(notebook, "Free destination");

    assertThrows(
        ApiException.class,
        () -> controller.moveFolder(notebook, trashed, folderMove(occupiedDestination.getId())));

    makeMe.refresh(trashed);
    assertThat(trashed.getParentFolder().getId(), equalTo(trash.getId()));
    assertThat(trashed.isTrashed(), equalTo(true));

    controller.moveFolder(notebook, trashed, folderMove(freeDestination.getId()));

    makeMe.refresh(trashed);
    makeMe.refresh(retained);
    assertThat(trashed.getParentFolder().getId(), equalTo(freeDestination.getId()));
    assertThat(retained.getFolder().getId(), equalTo(trashed.getId()));
  }

  @Test
  void folderNotInNotebookReturns404() {
    Folder folderInB = ownedFolder(ownedNotebook(), "Only B");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(ownedNotebook(), folderInB, folderMove(null)));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
  }

  @Test
  void rejectsParentNotFound() {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "Movable");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(nb, folder, folderMove(-99999)));
    assertThat(ex.getReason(), equalTo("Parent folder not found."));
  }

  @Test
  void rejectsParentInOtherNotebook() {
    Notebook nbA = ownedNotebook();
    Folder folder = ownedFolder(nbA, "Movable");
    Folder parentInB = ownedFolder(ownedNotebook(), "Foreign");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(nbA, folder, folderMove(parentInB.getId())));
    assertThat(ex.getReason(), equalTo("Parent folder not in notebook."));
  }

  @Test
  void mergesIntoSameNameDestinationWhenMergeRequested() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder target = ownedFolder(nb, "Dup");
    makeMe.aNote("NoteInTarget").folder(target).please();
    Folder holder = ownedFolder(nb, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Note noteInSource = makeMe.aNote("NoteInSource").folder(source).please();

    Folder result = controller.moveFolder(nb, source, folderMerge(null));

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(noteInSource);
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
    assertTrue(listingHasFolder(nb, null, target));
    assertThat(listingHasFolder(nb, null, source), equalTo(false));
  }

  @Test
  void mergesRecursivelyOnNestedNameClash() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder target = ownedFolder(nb, "Dup");
    Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
    Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
    Folder holder = ownedFolder(nb, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
    Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

    controller.moveFolder(nb, source, folderMerge(null));

    makeMe.refresh(deepNoteInTarget);
    makeMe.refresh(deepNoteInSource);
    assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
  }
}
