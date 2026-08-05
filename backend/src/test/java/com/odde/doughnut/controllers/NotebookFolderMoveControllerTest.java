package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderMoveControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void movesChildFolderToNotebookRoot() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
    Folder child = makeMe.aFolder().parentFolder(parent).name("Child").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(null);
    Folder result = controller.moveFolder(nb, child, req);
    assertThat(result.getName(), equalTo("Child"));

    FolderListing root = controller.listNotebookFolderListing(nb, null);
    assertTrue(root.folders().stream().anyMatch(f -> f.getId().equals(child.getId())));
    FolderListing underParent = controller.listNotebookFolderListing(nb, parent.getId());
    assertTrue(underParent.folders().stream().noneMatch(f -> f.getId().equals(child.getId())));
  }

  @Test
  void rejectsMoveIntoDescendant() {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder inner = makeMe.aFolder().parentFolder(outer).name("Inner").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(inner.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, outer, req));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Cannot move folder into its descendant."));
  }

  @Test
  void rejectsSelfAsDestination() {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Solo").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(folder.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, folder, req));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
  }

  @Test
  void rejectsDuplicateNameAtDestination() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    makeMe.aFolder().notebook(nb).name("Dup").please();
    Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
    Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(null);
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.moveFolder(nb, nestedDup, req));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
  }

  @Test
  void folderNotInNotebookReturns404() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderInB = makeMe.aFolder().notebook(nbB).name("Only B").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.moveFolder(nbA, folderInB, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
  }

  @Test
  void rejectsParentNotFound() {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Movable").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(-99999);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nb, folder, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Parent folder not found."));
  }

  @Test
  void rejectsParentInOtherNotebook() {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nbA).name("Movable").please();
    Folder parentInB = makeMe.aFolder().notebook(nbB).name("Foreign").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(parentInB.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nbA, folder, req));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Parent folder not in notebook."));
  }

  @Test
  void mergesIntoSameNameDestinationWhenMergeRequested() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder target = makeMe.aFolder().notebook(nb).name("Dup").please();
    Note noteInTarget = makeMe.aNote("NoteInTarget").folder(target).please();
    Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Note noteInSource = makeMe.aNote("NoteInSource").folder(source).please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(null);
    req.setMerge(true);
    Folder result = controller.moveFolder(nb, source, req);

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(noteInTarget);
    makeMe.refresh(noteInSource);
    assertThat(noteInTarget.getFolder().getId(), equalTo(target.getId()));
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
    FolderListing root = controller.listNotebookFolderListing(nb, null);
    assertTrue(root.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
    assertTrue(root.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
  }

  @Test
  void mergesRecursivelyOnNestedNameClash() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder target = makeMe.aFolder().notebook(nb).name("Dup").please();
    Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
    Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
    Folder holder = makeMe.aFolder().notebook(nb).name("Holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
    Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(null);
    req.setMerge(true);
    controller.moveFolder(nb, source, req);

    makeMe.refresh(deepNoteInTarget);
    makeMe.refresh(deepNoteInSource);
    assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
  }
}
