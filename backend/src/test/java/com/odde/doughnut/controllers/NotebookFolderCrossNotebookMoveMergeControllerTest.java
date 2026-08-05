package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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

class NotebookFolderCrossNotebookMoveMergeControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Test
  void mergesRecursivelyAcrossNotebooksWhenMergeRequested()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder target = makeMe.aFolder().notebook(nbB).name("Dup").please();
    Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
    Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
    Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
    Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(null);
    req.setMerge(true);
    Folder result = controller.moveFolder(nbA, source, req);

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(deepNoteInTarget);
    makeMe.refresh(deepNoteInSource);
    assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInTarget.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(deepNoteInSource.getNotebook().getId(), equalTo(nbB.getId()));
    FolderListing rootB = controller.listNotebookFolderListing(nbB, null);
    assertTrue(rootB.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
    assertTrue(rootB.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
    FolderListing rootA = controller.listNotebookFolderListing(nbA, null);
    assertTrue(rootA.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
  }

  @Test
  void rejectsCrossNotebookMergeWhenSoftDeletedNoteHasSameTitleAtDestinationFolder()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder target = makeMe.aFolder().notebook(nbB).name("Dup").please();
    Note deleted = makeMe.aNote().folder(target).title("ConflictTitle").please();
    noteService.destroy(deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, owner);

    Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    makeMe.aNote().folder(source).title("ConflictTitle").please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(null);
    req.setMerge(true);
    ApiException ex =
        assertThrows(ApiException.class, () -> controller.moveFolder(nbA, source, req));
    assertThat(
        ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        ex.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(deleted.getId())));
    makeMe.refresh(source);
    assertThat(source.getNotebook().getId(), equalTo(nbA.getId()));
  }

  @Test
  void mergesAcrossNotebooksWhenNoSoftDeletedTitleConflictAtDestination()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder parentP = makeMe.aFolder().notebook(nbB).name("P").please();
    Folder target = makeMe.aFolder().parentFolder(parentP).name("Dup").please();
    Note noteInTarget = makeMe.aNote("KeptInTarget").folder(target).please();
    Folder holder = makeMe.aFolder().notebook(nbA).name("Holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Note noteInSource = makeMe.aNote("FromSource").folder(source).please();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    req.setNewParentFolderId(parentP.getId());
    req.setMerge(true);
    Folder result = controller.moveFolder(nbA, source, req);

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(noteInTarget);
    makeMe.refresh(noteInSource);
    assertThat(noteInTarget.getFolder().getId(), equalTo(target.getId()));
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
    assertThat(noteInTarget.getNotebook().getId(), equalTo(nbB.getId()));
    assertThat(noteInSource.getNotebook().getId(), equalTo(nbB.getId()));
    FolderListing underP = controller.listNotebookFolderListing(nbB, parentP.getId());
    assertTrue(underP.folders().stream().anyMatch(f -> f.getId().equals(target.getId())));
    assertTrue(underP.folders().stream().noneMatch(f -> f.getId().equals(source.getId())));
  }
}
