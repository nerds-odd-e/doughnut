package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderCrossNotebookMoveShorthandCardinalityControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired NoteController noteController;

  @Test
  void crossNotebookFolderMove_reresolvesShorthandCardinalityInSourceAndDestination()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook source = ownedNotebook("Source NB");
    Notebook destination = ownedNotebook("Dest NB");

    makeMe.aNote("Target").notebook(source).please();
    Folder movedFolder = ownedFolder(source, "Moved");
    makeMe.aNote("Target").folder(movedFolder).please();
    Note sourceReferrer =
        makeMe.aNote("SourceReferrer").notebook(source).content("See [[Target]].").please();

    makeMe.aNote("Target").notebook(destination).please();
    Note destReferrer =
        makeMe.aNote("DestReferrer").notebook(destination).content("See [[Target]].").please();

    assertThat(
        noteController.showNote(sourceReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
    assertThat(
        noteController.showNote(destReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.moveFolder(source, movedFolder, folderMoveTo(destination, null));

    assertThat(
        noteController.showNote(sourceReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(
        noteController.showNote(destReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }
}
