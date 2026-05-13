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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired RelationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class MoveNoteToFolderTest {
    User anotherUser;
    Note ownNote;
    Folder targetFolder;

    @BeforeEach
    void setup() {
      anotherUser = makeMe.aUser().please();
      Notebook ownNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      ownNote = makeMe.aNote("flower").inNotebook(ownNotebook).please();
      Notebook anchorNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aRootNote("nbroot").inNotebook(anchorNotebook).please();
      targetFolder = makeMe.aFolder().notebook(anchorNotebook).name("TargetF").please();
    }

    @Test
    void moveNoteToFolderSuccessfully() throws UnexpectedNoAccessRightException {
      Note mover = makeMe.aNote("mover").inNotebook(ownNote.getNotebook()).please();
      var result = controller.moveNoteToFolder(mover, targetFolder);
      assertThat(result, hasSize(1));
      mover = noteRepository.findById(mover.getId()).orElseThrow();
      assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNoteToFolder() {
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
      Note mover = makeMe.aNote().inNotebook(otherNotebook).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, targetFolder));
    }

    @Test
    void shouldNotAllowMoveToUnauthorizedFolderNotebook() {
      Notebook moverNotebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note mover = makeMe.aNote().inNotebook(moverNotebook).please();
      Notebook otherNotebook = makeMe.aNotebook().creatorAndOwner(anotherUser).please();
      Note otherAnchor = makeMe.aRootNote("other").inNotebook(otherNotebook).please();
      Folder otherFolder = makeMe.aFolder().notebook(otherNotebook).name("ForeignF").please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, otherFolder));
    }

    @Test
    void moveNoteIntoFolder_collectsPeersInFolder() throws Throwable {
      User u = currentUser.getUser();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("top").inNotebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
      Note peerA = makeMe.aNote("A").inNotebook(notebook).please();
      Note peerB = makeMe.aNote("B").inNotebook(notebook).please();
      Note mover = makeMe.aNote("M").inNotebook(notebook).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(peerA, folder);
      controller.moveNoteToFolder(peerB, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToFolder(mover, folder);
      makeMe.refresh(mover);
      assertThat(mover.getFolder().getId(), equalTo(folder.getId()));
      List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
      assertThat(
          ordered.stream().map(Note::getId).toList(),
          containsInAnyOrder(peerA.getId(), peerB.getId(), mover.getId()));
    }

    @Test
    void moveNoteToSameFolder_isIdempotent() throws Throwable {
      User u = currentUser.getUser();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("top2").inNotebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F2").please();
      Note peerA = makeMe.aNote("A2").inNotebook(notebook).please();
      Note peerB = makeMe.aNote("B2").inNotebook(notebook).please();
      Note mover = makeMe.aNote("M2").inNotebook(notebook).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(peerA, folder);
      controller.moveNoteToFolder(peerB, folder);
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToFolder(mover, folder);
      List<Note> ordered = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
      assertThat(
          ordered.stream().map(Note::getId).toList(),
          containsInAnyOrder(peerA.getId(), peerB.getId(), mover.getId()));
    }
  }

  @Nested
  class MoveNoteToNotebookRootTest {
    @Test
    void moveNoteToNotebookRoot_checksAccessOnNote() {
      Note foreign = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.moveNoteToNotebookRoot(foreign));
    }

    @Test
    void moveNoteToNotebookRoot_clearsFolder() throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("top").inNotebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
      Note mover = makeMe.aNote("M").inNotebook(notebook).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRoot(mover);
      makeMe.refresh(mover);
      assertThat(mover.getFolder(), nullValue());
    }
  }

  @Nested
  class MoveNoteToNotebookRootInNotebookTest {
    @Test
    void moveNoteToNotebookRootInNotebook_movesToTargetNotebookRoot()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("nb1").inNotebook(nb1).please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("nb2").inNotebook(nb2).please();
      Folder folder = makeMe.aFolder().notebook(nb1).name("F").please();
      Note mover = makeMe.aNote("M").inNotebook(nb1).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      Notebook targetNb = nb2;
      controller.moveNoteToNotebookRootInNotebook(mover, targetNb);
      makeMe.refresh(mover);
      assertThat(mover.getFolder(), nullValue());
      assertThat(mover.getNotebook().getId(), equalTo(targetNb.getId()));
    }

    @Test
    void moveNoteToNotebookRootInNotebook_rejectsUnauthorizedTargetNotebook() {
      User u = currentUser.getUser();
      User other = makeMe.aUser().please();
      Notebook nb1 = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("mine").inNotebook(nb1).please();
      Notebook theirsNb = makeMe.aNotebook().creatorAndOwner(other).please();
      makeMe.aRootNote("theirs").inNotebook(theirsNb).please();
      Note mover = makeMe.aNote("M").inNotebook(nb1).please();
      Notebook foreignNb = makeMe.aNotebook().creatorAndOwner(other).please();
      makeMe.aNote().inNotebook(foreignNb).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToNotebookRootInNotebook(mover, foreignNb));
    }
  }
}
