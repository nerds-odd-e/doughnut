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
import com.odde.doughnut.testability.builders.NoteBuilder;
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
      NoteBuilder noteBuilder1 = makeMe.aNote("flower");
      ownNote = noteBuilder1.nbCreatorAndOwner(currentUser.getUser()).please();
      NoteBuilder noteBuilder = makeMe.aRootNote("nbroot");
      Note anchor = noteBuilder.nbCreatorAndOwner(currentUser.getUser()).please();
      targetFolder = makeMe.aFolder().notebook(anchor.getNotebook()).name("TargetF").please();
    }

    @Test
    void moveNoteToFolderSuccessfully() throws UnexpectedNoAccessRightException {
      Note mover = makeMe.aNote("mover").underSameNotebookAs(ownNote).please();
      var result = controller.moveNoteToFolder(mover, targetFolder);
      assertThat(result, hasSize(1));
      mover = noteRepository.findById(mover.getId()).orElseThrow();
      assertThat(mover.getFolder().getId(), equalTo(targetFolder.getId()));
    }

    @Test
    void shouldNotAllowMoveOtherPeoplesNoteToFolder() {
      NoteBuilder noteBuilder = makeMe.aNote();
      Note mover = noteBuilder.nbCreatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, targetFolder));
    }

    @Test
    void shouldNotAllowMoveToUnauthorizedFolderNotebook() {
      NoteBuilder noteBuilder1 = makeMe.aNote();
      Note mover = noteBuilder1.nbCreatorAndOwner(currentUser.getUser()).please();
      NoteBuilder noteBuilder = makeMe.aRootNote("other");
      Note otherAnchor = noteBuilder.nbCreatorAndOwner(anotherUser).please();
      Folder otherFolder =
          makeMe.aFolder().notebook(otherAnchor.getNotebook()).name("ForeignF").please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, otherFolder));
    }

    @Test
    void moveNoteIntoFolder_collectsPeersInFolder() throws Throwable {
      User u = currentUser.getUser();
      NoteBuilder noteBuilder = makeMe.aRootNote("top");
      Note anchor = noteBuilder.nbCreatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(anchor.getNotebook()).name("F").please();
      Note peerA = makeMe.aNote("A").underSameNotebookAs(anchor).please();
      Note peerB = makeMe.aNote("B").underSameNotebookAs(anchor).please();
      Note mover = makeMe.aNote("M").underSameNotebookAs(anchor).please();
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
      NoteBuilder noteBuilder = makeMe.aRootNote("top2");
      Note anchor = noteBuilder.nbCreatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(anchor.getNotebook()).name("F2").please();
      Note peerA = makeMe.aNote("A2").underSameNotebookAs(anchor).please();
      Note peerB = makeMe.aNote("B2").underSameNotebookAs(anchor).please();
      Note mover = makeMe.aNote("M2").underSameNotebookAs(anchor).please();
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
      NoteBuilder noteBuilder = makeMe.aRootNote("top");
      Note root = noteBuilder.nbCreatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(root.getNotebook()).name("F").please();
      Note mover = makeMe.aNote("M").underSameNotebookAs(root).please();
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
      NoteBuilder noteBuilder1 = makeMe.aRootNote("nb1");
      Note nb1Root = noteBuilder1.nbCreatorAndOwner(u).please();
      NoteBuilder noteBuilder = makeMe.aRootNote("nb2");
      Note nb2Root = noteBuilder.nbCreatorAndOwner(u).please();
      Folder folder = makeMe.aFolder().notebook(nb1Root.getNotebook()).name("F").please();
      Note mover = makeMe.aNote("M").underSameNotebookAs(nb1Root).please();
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(mover, folder);
      makeMe.entityPersister.flush();

      Notebook targetNb = nb2Root.getNotebook();
      controller.moveNoteToNotebookRootInNotebook(mover, targetNb);
      makeMe.refresh(mover);
      assertThat(mover.getFolder(), nullValue());
      assertThat(mover.getNotebook().getId(), equalTo(targetNb.getId()));
    }

    @Test
    void moveNoteToNotebookRootInNotebook_rejectsUnauthorizedTargetNotebook() {
      User u = currentUser.getUser();
      User other = makeMe.aUser().please();
      NoteBuilder noteBuilder2 = makeMe.aRootNote("mine");
      Note nb1Root = noteBuilder2.nbCreatorAndOwner(u).please();
      NoteBuilder noteBuilder1 = makeMe.aRootNote("theirs");
      noteBuilder1.nbCreatorAndOwner(other).please();
      Note mover = makeMe.aNote("M").underSameNotebookAs(nb1Root).please();
      NoteBuilder noteBuilder = makeMe.aNote();
      Notebook foreignNb = noteBuilder.nbCreatorAndOwner(other).please().getNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToNotebookRootInNotebook(mover, foreignNb));
    }
  }
}
