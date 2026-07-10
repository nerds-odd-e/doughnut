package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerMoveNoteToFolderTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired RelationController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  User anotherUser;
  Note ownNote;
  Folder targetFolder;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    anotherUser = makeMe.aUser().please();
    Notebook ownNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    ownNote = makeMe.aNote("flower").notebook(ownNotebook).please();
    Notebook anchorNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    makeMe.aRootNote("nbroot").notebook(anchorNotebook).please();
    targetFolder = makeMe.aFolder().notebook(anchorNotebook).name("TargetF").please();
  }

  @Test
  void moveNoteToFolderSuccessfully() throws UnexpectedNoAccessRightException {
    Note mover = makeMe.aNote("mover").notebook(ownNote.getNotebook()).please();
    var result = controller.moveNoteToFolder(mover, targetFolder);
    assertThat(result, hasSize(1));
    mover = noteRepository.findById(mover.getId()).orElseThrow();
    assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
  }

  @Test
  void shouldNotAllowMoveOtherPeoplesNoteToFolder() {
    Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
    Note mover = makeMe.aNote().notebook(otherNotebook).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.moveNoteToFolder(mover, targetFolder));
  }

  @Test
  void shouldNotAllowMoveToUnauthorizedFolderNotebook() {
    Notebook moverNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Note mover = makeMe.aNote().notebook(moverNotebook).please();
    Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
    makeMe.aRootNote("other").notebook(otherNotebook).please();
    Folder otherFolder = makeMe.aFolder().notebook(otherNotebook).name("ForeignF").please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.moveNoteToFolder(mover, otherFolder));
  }

  @Test
  void moveNoteIntoFolder_collectsPeersAndIsIdempotent() throws Throwable {
    User u = currentUser.getUser();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(u).please();
    makeMe.aRootNote("top").notebook(notebook).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
    Note peer = makeMe.aNote("A").notebook(notebook).please();
    Note mover = makeMe.aNote("M").notebook(notebook).please();
    makeMe.entityPersister.flush();
    controller.moveNoteToFolder(peer, folder);
    controller.moveNoteToFolder(mover, folder);
    makeMe.refresh(mover);
    assertThat(mover.getFolder().getId(), equalTo(folder.getId()));

    controller.moveNoteToFolder(mover, folder);
    List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    assertThat(
        ordered.stream().map(Note::getId).toList(),
        containsInAnyOrder(peer.getId(), mover.getId()));
  }

  @Test
  void sameNotebookMoveToFolder_doesNotRewriteLinks() throws UnexpectedNoAccessRightException {
    User u = currentUser.getUser();
    Notebook notebook = makeMe.aNotebook().name("SameNb").creatorAndOwner(u).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
    Note target = makeMe.aNote("X").notebook(notebook).please();
    Note mover = makeMe.aNote("Mover").notebook(notebook).please();
    mover.setContent("See [[X]].");
    Note referrer = makeMe.aNote("Carrier").notebook(notebook).please();
    referrer.setContent("[[Mover]]");
    makeMe.entityPersister.flush();
    wikiTitleCacheServiceBean.refreshForNote(referrer, u);
    wikiTitleCacheServiceBean.refreshForNote(mover, u);
    makeMe.entityPersister.flush();

    controller.moveNoteToFolder(mover, folder);

    makeMe.refresh(referrer);
    makeMe.refresh(mover);
    assertThat(referrer.getContent(), equalTo("[[Mover]]"));
    assertThat(mover.getContent(), equalTo("See [[X]]."));
    assertThat(
        wikiTitleCacheServiceBean.wikiTitlesForViewer(mover, u).stream()
            .map(wt -> wt.getNoteId())
            .toList(),
        containsInAnyOrder(target.getId()));
  }

  @Test
  void crossNotebookMoveToFolder_preservesNullContentWhenOutgoingRewriteHasNothingToDo()
      throws UnexpectedNoAccessRightException {
    User u = currentUser.getUser();
    Notebook oldNotebook = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
    Notebook newNotebook = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
    Folder destination = makeMe.aFolder().notebook(newNotebook).name("Dest").please();
    Note mover = makeMe.aNote("Mover").notebook(oldNotebook).content(null).please();
    makeMe.entityPersister.flush();
    makeMe.refresh(mover);
    Timestamp originalUpdatedAt = mover.getUpdatedAt();

    controller.moveNoteToFolder(mover, destination);

    makeMe.refresh(mover);
    assertThat(mover.getContent(), nullValue());
    assertThat(mover.getUpdatedAt(), equalTo(originalUpdatedAt));
  }
}
