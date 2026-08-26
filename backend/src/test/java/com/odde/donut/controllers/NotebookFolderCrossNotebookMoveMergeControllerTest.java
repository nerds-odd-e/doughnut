package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookFolderCrossNotebookMoveMergeControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Test
  void mergesRecursivelyAcrossNotebooksWhenMergeRequested()
      throws UnexpectedNoAccessRightException {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder target = ownedFolder(nbB, "Dup");
    Folder innerTarget = makeMe.aFolder().parentFolder(target).name("Inner").please();
    Note deepNoteInTarget = makeMe.aNote("DeepTarget").folder(innerTarget).please();
    Folder holder = ownedFolder(nbA, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Folder innerSource = makeMe.aFolder().parentFolder(source).name("Inner").please();
    Note deepNoteInSource = makeMe.aNote("DeepSource").folder(innerSource).please();

    Folder result = controller.moveFolder(nbA, source, folderMergeTo(nbB, null));

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(deepNoteInTarget);
    makeMe.refresh(deepNoteInSource);
    assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getNotebook().getId(), equalTo(nbB.getId()));
    assertTrue(listingHasFolder(nbB, null, target));
    assertThat(listingHasFolder(nbB, null, source), equalTo(false));
    assertThat(listingHasFolder(nbA, null, source), equalTo(false));
  }

  @Test
  void rejectsCrossNotebookMergeWhenSoftDeletedNoteHasSameTitleAtDestinationFolder() {
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder target = ownedFolder(nbB, "Dup");
    Note deleted = makeMe.aNote().folder(target).title("ConflictTitle").please();
    noteService.destroy(
        deleted, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

    Folder holder = ownedFolder(nbA, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    makeMe.aNote().folder(source).title("ConflictTitle").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> controller.moveFolder(nbA, source, folderMergeTo(nbB, null)));
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
    Notebook nbA = ownedNotebook();
    Notebook nbB = ownedNotebook();
    Folder parentP = ownedFolder(nbB, "P");
    Folder target = makeMe.aFolder().parentFolder(parentP).name("Dup").please();
    makeMe.aNote("KeptInTarget").folder(target).please();
    Folder holder = ownedFolder(nbA, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Note noteInSource = makeMe.aNote("FromSource").folder(source).please();

    Folder result = controller.moveFolder(nbA, source, folderMergeTo(nbB, parentP.getId()));

    assertThat(result.getId(), equalTo(target.getId()));
    makeMe.refresh(noteInSource);
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
    assertThat(noteInSource.getNotebook().getId(), equalTo(nbB.getId()));
    assertTrue(listingHasFolder(nbB, parentP.getId(), target));
    assertThat(listingHasFolder(nbB, parentP.getId(), source), equalTo(false));
  }
}
