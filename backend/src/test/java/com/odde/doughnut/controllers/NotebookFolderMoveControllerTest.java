package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderMoveControllerTest extends NotebookFolderManagementControllerTestBase {

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
