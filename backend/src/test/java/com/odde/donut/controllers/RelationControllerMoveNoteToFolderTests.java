package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerMoveNoteToFolderTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired NoteController noteController;
  @Autowired RelationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
  }

  private Folder ownedFolder(String name) {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    return makeMe.aFolder().notebook(notebook).name(name).please();
  }

  @Test
  void moveNoteToFolderSuccessfully() throws UnexpectedNoAccessRightException {
    Note mover = makeMe.aNote("mover").notebookOwnedBy(currentUser.getUser()).please();
    Folder targetFolder = ownedFolder("TargetF");

    var result = controller.moveNoteToFolder(mover, targetFolder);

    assertThat(result, hasSize(1));
    makeMe.refresh(mover);
    assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
  }

  @Test
  void shouldNotAllowMoveOtherPeoplesNoteToFolder() {
    Folder targetFolder = ownedFolder("TargetF");
    Note mover = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.moveNoteToFolder(mover, targetFolder));
  }

  @Test
  void shouldNotAllowMoveToUnauthorizedFolderNotebook() {
    Note mover = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    Folder otherFolder = makeMe.aFolder().notebook(otherNotebook).name("ForeignF").please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.moveNoteToFolder(mover, otherFolder));
  }

  @Test
  void moveNoteIntoFolder_collectsPeersAndIsIdempotent() throws Throwable {
    Note peer = makeMe.aNote("A").notebookOwnedBy(currentUser.getUser()).please();
    Note mover = makeMe.aNote("M").underSameNotebookAs(peer).please();
    Folder folder = makeMe.aFolder().notebook(peer.getNotebook()).name("F").please();

    controller.moveNoteToFolder(peer, folder);
    controller.moveNoteToFolder(mover, folder);
    controller.moveNoteToFolder(mover, folder);

    List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    assertThat(
        ordered.stream().map(Note::getId).toList(),
        containsInAnyOrder(peer.getId(), mover.getId()));
  }

  @Test
  void sameNotebookMoveToFolder_doesNotRewriteLinks() throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook("SameNb");
    Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
    makeMe.aNote("X").notebook(notebook).please();
    Note mover = makeMe.aNote("Mover").notebook(notebook).content("See [[X]].").please();
    Note referrer =
        makeMe.aNote("Carrier").underSameNotebookAs(mover).content("[[Mover]]").please();

    controller.moveNoteToFolder(mover, folder);

    makeMe.refresh(referrer);
    makeMe.refresh(mover);
    assertThat(referrer.getContent(), equalTo("[[Mover]]"));
    assertThat(mover.getContent(), equalTo("See [[X]]."));
  }

  @Test
  void crossNotebookMoveToFolder_preservesNullContentWhenOutgoingRewriteHasNothingToDo()
      throws UnexpectedNoAccessRightException {
    Notebook oldNotebook = ownedNotebook("OldNb");
    Folder destination = makeMe.aFolder().notebook(ownedNotebook("NewNb")).name("Dest").please();
    Note mover = makeMe.aNote("Mover").notebook(oldNotebook).content(null).please();
    makeMe.refresh(mover);
    Timestamp originalUpdatedAt = mover.getUpdatedAt();

    controller.moveNoteToFolder(mover, destination);

    makeMe.refresh(mover);
    assertThat(mover.getContent(), nullValue());
    assertThat(mover.getUpdatedAt(), equalTo(originalUpdatedAt));
  }

  @Test
  void crossNotebookMoveToFolder_reresolvesDestinationNotebookCardinalityLive()
      throws UnexpectedNoAccessRightException {
    Notebook destNotebook = ownedNotebook("Dest NB");
    Note destTarget = makeMe.aNote("Target").notebook(destNotebook).please();
    Note destReferrer =
        makeMe.aNote("DestReferrer").underSameNotebookAs(destTarget).content("[[Target]]").please();
    assertThat(
        noteController.showNote(destReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    Note movedTarget = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Folder destFolder = makeMe.aFolder().notebook(destNotebook).name("F").please();

    controller.moveNoteToFolder(movedTarget, destFolder);

    assertThat(
        noteController.showNote(destReferrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }
}
