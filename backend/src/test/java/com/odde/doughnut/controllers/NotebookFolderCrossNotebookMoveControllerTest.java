package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.ApiError;
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
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder folderF = ownedFolder(nbA, "F");
    Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
    Note noteInF = makeMe.aNote("InF").folder(folderF).please();
    Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();

    Folder result = controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null));

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
    assertTrue(listingHasFolder(nbB, null, folderF));
    assertThat(listingHasFolder(nbA, null, folderF), equalTo(false));
  }

  @Test
  void rejectsCrossNotebookMoveWithoutDestinationNotebookAccess() {
    User owner = makeMe.aUser().please();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();

    currentUser.setUser(owner);
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null)));
  }

  @Test
  void movesFolderSubtreeIntoFolderInAnotherNotebook() throws UnexpectedNoAccessRightException {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder folderF = ownedFolder(nbA, "F");
    Folder subfolder = makeMe.aFolder().parentFolder(folderF).name("Child").please();
    Note noteInSub = makeMe.aNote("InSub").folder(subfolder).please();
    Folder parentP = ownedFolder(nbB, "P");

    Folder result = controller.moveFolder(nbA, folderF, folderMoveTo(nbB, parentP.getId()));

    assertThat(result.getId(), equalTo(folderF.getId()));
    makeMe.refresh(folderF);
    makeMe.refresh(noteInSub);

    assertThat(folderF.getParentFolder().getId(), equalTo(parentP.getId()));
    assertThat(noteInSub.getNotebook().getId(), equalTo(nbB.getId()));
    assertTrue(listingHasFolder(nbB, parentP.getId(), folderF));
    assertThat(listingHasFolder(nbA, null, folderF), equalTo(false));
  }

  @Test
  void rejectsDuplicateNameAtDestinationNotebookRoot() {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    ownedFolder(nbB, "Dup");
    Folder holder = ownedFolder(nbA, "Holder");
    Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.moveFolder(nbA, nestedDup, folderMoveTo(nbB, null)));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    makeMe.refresh(nestedDup);
    assertThat(nestedDup.getNotebook().getId(), equalTo(nbA.getId()));
  }

  @Test
  void rejectsDuplicateNameAtDestinationParentFolder() {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder parentP = ownedFolder(nbB, "P");
    makeMe.aFolder().parentFolder(parentP).name("F").please();
    Folder folderF = ownedFolder(nbA, "F");

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.moveFolder(nbA, folderF, folderMoveTo(nbB, parentP.getId())));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    makeMe.refresh(folderF);
    assertThat(folderF.getNotebook().getId(), equalTo(nbA.getId()));
    assertThat(folderF.getParentFolder(), nullValue());
  }

  @Test
  void rejectsCrossNotebookMoveWhenSoftDeletedNoteHasSameTitleAtDestination()
      throws UnexpectedNoAccessRightException {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder folderF = ownedFolder(nbB, "F");
    Note deleted = makeMe.aNote().folder(folderF).title("DupTitle").please();
    noteService.destroy(
        deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

    controller.moveFolder(nbB, folderF, folderMoveTo(nbA, null));
    makeMe.aNote().folder(folderF).title("DupTitle").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null)));
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
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder folderF = ownedFolder(nbA, "F");

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.moveFolder(nbA, folderF, folderMoveTo(nbB, folderF.getId())));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertThat(ex.getReason(), equalTo("Cannot move folder into itself."));
  }
}
