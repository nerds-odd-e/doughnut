package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookFolderMoveNameClashControllerTest extends NotebookFolderManagementControllerTestBase {

  @Test
  void rejectsDuplicateNameAtDestination() {
    Notebook nb = ownedNotebook();
    ownedFolder(nb, "Dup");
    Folder holder = ownedFolder(nb, "Holder");
    Folder nestedDup = makeMe.aFolder().parentFolder(holder).name("Dup").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> folderController.moveFolder(nb, nestedDup, folderMove(null)));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
  }

  @Test
  void mergesIntoSameNameDestinationWhenMergeRequested() throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder target = ownedFolder(nb, "Dup");
    makeMe.aNote("NoteInTarget").folder(target).please();
    Folder holder = ownedFolder(nb, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Dup").please();
    Note noteInSource = makeMe.aNote("NoteInSource").folder(source).please();

    Folder result = folderController.moveFolder(nb, source, folderMerge(null));

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

    folderController.moveFolder(nb, source, folderMerge(null));

    makeMe.refresh(deepNoteInTarget);
    makeMe.refresh(deepNoteInSource);
    assertThat(deepNoteInTarget.getFolder().getId(), equalTo(innerTarget.getId()));
    assertThat(deepNoteInSource.getFolder().getId(), equalTo(innerTarget.getId()));
  }

  @Test
  void mergesIntoACaseVariantDestinationFolderKeepingItsName()
      throws UnexpectedNoAccessRightException {
    Notebook nb = ownedNotebook();
    Folder target = ownedFolder(nb, "diagrams");
    Folder holder = ownedFolder(nb, "Holder");
    Folder source = makeMe.aFolder().parentFolder(holder).name("Diagrams").please();
    Note noteInSource = makeMe.aNote("Sketch").folder(source).please();

    Folder result = folderController.moveFolder(nb, source, folderMerge(null));

    assertThat(result.getId(), equalTo(target.getId()));
    assertThat(result.getName(), equalTo("diagrams"));
    makeMe.refresh(noteInSource);
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
  }

  @Test
  void refusesMovingOntoAFileNameIgnoringCaseWithoutOfferingMerge() {
    Notebook nb = ownedNotebook();
    Folder physics = ownedFolder(nb, "physics");
    makeMe.anAttachment("Force.png").in(physics).please();
    Folder moved = ownedFolder(nb, "force.png");

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> folderController.moveFolder(nb, moved, folderMerge(physics.getId())));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(ex.getErrorBody().getMessage(), containsString("physics/Force.png"));
    makeMe.refresh(moved);
    assertThat(moved.getParentFolder(), nullValue());
  }

  @Test
  void refusesMovingOntoACaseVariantFolderInAnotherNotebook() {
    Notebook source = ownedNotebook();
    Folder moved = ownedFolder(source, "shared");
    Notebook destination = ownedNotebook();
    ownedFolder(destination, "Shared");

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> folderController.moveFolder(source, moved, folderMoveTo(destination, null)));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(
        ex.getErrorBody().getMessage(), equalTo("A folder with this name already exists here."));
    makeMe.refresh(moved);
    assertThat(moved.getNotebook().getId(), equalTo(source.getId()));
  }

  @Test
  void mergesIntoACaseVariantFolderInAnotherNotebookKeepingItsName()
      throws UnexpectedNoAccessRightException {
    Notebook source = ownedNotebook();
    Folder moved = ownedFolder(source, "shared");
    Note noteInSource = makeMe.aNote("Intro").folder(moved).please();
    Notebook destination = ownedNotebook();
    Folder target = ownedFolder(destination, "Shared");

    Folder result = folderController.moveFolder(source, moved, folderMergeTo(destination, null));

    assertThat(result.getId(), equalTo(target.getId()));
    assertThat(result.getName(), equalTo("Shared"));
    makeMe.refresh(noteInSource);
    assertThat(noteInSource.getFolder().getId(), equalTo(target.getId()));
  }

  @Test
  void refusesMovingOntoAFileNameInAnotherNotebook() {
    Notebook source = ownedNotebook();
    Folder moved = ownedFolder(source, "shared");
    Notebook destination = ownedNotebook();
    makeMe.anAttachment("shared").atRootOf(destination).please();

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> folderController.moveFolder(source, moved, folderMoveTo(destination, null)));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(ex.getErrorBody().getMessage(), containsString("shared"));
    makeMe.refresh(moved);
    assertThat(moved.getNotebook().getId(), equalTo(source.getId()));
  }

  @Test
  void refusesMergingANoteOntoACaseVariantNoteInAnotherNotebook() {
    Notebook source = ownedNotebook();
    Folder moved = ownedFolder(source, "shared");
    Note movedNote = makeMe.aNote("Intro").folder(moved).please();
    Notebook destination = ownedNotebook();
    Folder target = ownedFolder(destination, "Shared");
    makeMe.aNote("intro").folder(target).please();

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> folderController.moveFolder(source, moved, folderMergeTo(destination, null)));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(ex.getErrorBody().getMessage(), containsString("Shared/intro.md"));
    makeMe.refresh(moved);
    makeMe.refresh(movedNote);
    assertThat(moved.getNotebook().getId(), equalTo(source.getId()));
    assertThat(movedNote.getFolder().getId(), equalTo(moved.getId()));
  }

  @Test
  void mergesASubfolderIntoACaseVariantFolderInAnotherNotebook()
      throws UnexpectedNoAccessRightException {
    Notebook source = ownedNotebook();
    Folder moved = ownedFolder(source, "Shared");
    Folder movedDeep = makeMe.aFolder().parentFolder(moved).name("Deep").please();
    Note deepNote = makeMe.aNote("Detail").folder(movedDeep).please();
    Notebook destination = ownedNotebook();
    Folder target = ownedFolder(destination, "Shared");
    Folder targetDeep = makeMe.aFolder().parentFolder(target).name("deep").please();

    folderController.moveFolder(source, moved, folderMergeTo(destination, null));

    makeMe.refresh(deepNote);
    assertThat(deepNote.getFolder().getId(), equalTo(targetDeep.getId()));
  }
}
