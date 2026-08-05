package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.FolderListing;
import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookFolderCrossNotebookMoveControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Test
  void movesFolderSubtreeToAnotherNotebookRoot() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
    Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
    Note noteInF = makeMe.aNote("InF").folder(folderF).please();
    Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(null);
    Folder result = controller.moveFolder(nbA, folderF, req);

    assertThat(result.getId(), equalTo(folderF.getId()));
    makeMe.refresh(folderF);
    makeMe.refresh(subfolder);
    makeMe.refresh(noteInF);
    makeMe.refresh(noteInSub);

    assertThat(folderF.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(subfolder.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(noteInF.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(noteInSub.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(folderF.getParentFolder(), nullValue());

    FolderListing rootB = controller.listNotebookFolderListing(nbB, null);
    assertTrue(rootB.folders().stream().anyMatch(f -> f.getId().equals(folderF.getId())));
    FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
    assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(folderF.getId())));
  }

  @Test
  void rejectsCrossNotebookMoveWithoutDestinationNotebookAccess() {
    User owner = makeMe.aUser().please();
    User other = makeMe.aUser().please();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(other).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

    currentUser.setUser(owner);
    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.moveFolder(nbA, folderF, req));
  }

  @Test
  void movesFolderSubtreeIntoFolderInAnotherNotebook() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
    Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
    Note noteInF = makeMe.aNote("InF").folder(folderF).please();
    Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();
    Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(parentP.getId());
    Folder result = controller.moveFolder(nbA, folderF, req);

    assertThat(result.getId(), equalTo(folderF.getId()));
    makeMe.refresh(folderF);
    makeMe.refresh(subfolder);
    makeMe.refresh(noteInF);
    makeMe.refresh(noteInSub);

    assertThat(folderF.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(subfolder.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(noteInF.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(noteInSub.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(folderF.getParentFolder().getId(), equalTo(parentP.getId()));

    FolderListing underP = controller.listNotebookFolderListing(nbB, parentP.getId());
    assertTrue(underP.folders().stream().anyMatch(f -> f.getId().equals(folderF.getId())));
    FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
    assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(folderF.getId())));
  }

  @Test
  void rejectsCrossNotebookMoveToTargetParentWithoutDestinationNotebookAccess() {
    User owner = makeMe.aUser().please();
    User other = makeMe.aUser().please();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(other).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
    Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();

    currentUser.setUser(owner);
    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(parentP.getId());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.moveFolder(nbA, folderF, req));
  }

  @Test
  void rejectsDuplicateNameAtDestinationNotebookRoot() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    makeMe.aFolder().notebook(nbB).name("Dup").please();
    Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
    Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(null);
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.moveFolder(nbA, nestedDup, req));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
    makeMe.refresh(nestedDup);
    assertThat(nestedDup.getNotebook().getId(), equalTo(nbA.getId()));
  }

  @Test
  void rejectsDuplicateNameAtDestinationParentFolder() throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();
    makeMe.aFolder().parentFolder(parentP).name("F").please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(parentP.getId());
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.moveFolder(nbA, folderF, req));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
    makeMe.refresh(folderF);
    assertThat(folderF.getNotebook().getId(), equalTo(nbA.getId()));
    assertThat(folderF.getParentFolder(), nullValue());
  }

  @Test
  void rejectsCrossNotebookMoveWhenSoftDeletedNoteHasSameTitleAtDestination()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderF = makeMe.aFolder().notebook(nbB).name("F").please();
    Note deleted = makeMe.aNote().folder(folderF).title("DupTitle").please();
    noteService.destroy(deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, owner);

    FolderMoveRequest moveToA = new FolderMoveRequest();
    moveToA.setDestinationNotebookId(nbA.getId());
    controller.moveFolder(nbB, folderF, moveToA);

    makeMe.aNote().folder(folderF).title("DupTitle").please();

    FolderMoveRequest moveBackToB = new FolderMoveRequest();
    moveBackToB.setDestinationNotebookId(nbB.getId());
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.moveFolder(nbA, folderF, moveBackToB));
    assertThat(
        ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        ex.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(deleted.getId())));
    makeMe.refresh(folderF);
    assertThat(folderF.getNotebook().getId(), equalTo(nbA.getId()));
  }

  @Test
  void rejectsCrossNotebookMoveIntoItself() {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(folderF.getId());
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.moveFolder(nbA, folderF, req));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
  }
}
