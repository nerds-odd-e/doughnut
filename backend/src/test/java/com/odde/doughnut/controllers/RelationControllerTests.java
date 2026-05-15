package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  @Autowired RelationController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

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
      Note otherAnchor = makeMe.aRootNote("other").notebook(otherNotebook).please();
      Folder otherFolder = makeMe.aFolder().notebook(otherNotebook).name("ForeignF").please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToFolder(mover, otherFolder));
    }

    @Test
    void moveNoteIntoFolder_collectsPeersInFolder() throws Throwable {
      User u = currentUser.getUser();
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("top").notebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
      Note peerA = makeMe.aNote("A").notebook(notebook).please();
      Note peerB = makeMe.aNote("B").notebook(notebook).please();
      Note mover = makeMe.aNote("M").notebook(notebook).please();
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
      makeMe.aRootNote("top2").notebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F2").please();
      Note peerA = makeMe.aNote("A2").notebook(notebook).please();
      Note peerB = makeMe.aNote("B2").notebook(notebook).please();
      Note mover = makeMe.aNote("M2").notebook(notebook).please();
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
      makeMe.aRootNote("top").notebook(notebook).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("F").please();
      Note mover = makeMe.aNote("M").notebook(notebook).please();
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
      makeMe.aRootNote("nb1").notebook(nb1).please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(u).please();
      makeMe.aRootNote("nb2").notebook(nb2).please();
      Folder folder = makeMe.aFolder().notebook(nb1).name("F").please();
      Note mover = makeMe.aNote("M").notebook(nb1).please();
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
      makeMe.aRootNote("mine").notebook(nb1).please();
      Notebook theirsNb = makeMe.aNotebook().creatorAndOwner(other).please();
      makeMe.aRootNote("theirs").notebook(theirsNb).please();
      Note mover = makeMe.aNote("M").notebook(nb1).please();
      Notebook foreignNb = makeMe.aNotebook().creatorAndOwner(other).please();
      makeMe.aNote().notebook(foreignNb).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToNotebookRootInNotebook(mover, foreignNb));
    }

    @Test
    void crossNotebookMove_qualifiesUnqualifiedReferrerLink()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root1").notebook(nb1).please();
      Notebook nb2 = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root2").notebook(nb2).please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();

      referrer.setContent("[[MyNote]]");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      List<NoteRealm> result = controller.moveNoteToNotebookRootInNotebook(target, nb2);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[NewNb:MyNote|MyNote]]"));
      assertThat(
          noteWikiTitleCacheRepository
              .findByNote_IdOrderByIdAsc(referrer.getId())
              .getFirst()
              .getLinkText(),
          equalTo("NewNb:MyNote|MyNote"));
      assertThat(result.getFirst().getReferences(), hasSize(1));
      assertThat(result.getFirst().getReferences().getFirst().getId(), equalTo(referrer.getId()));
    }

    @Test
    void crossNotebookMove_requalifiesQualifiedReferrerLink()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root1").notebook(nb1).please();
      Notebook nb2 = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root2").notebook(nb2).please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();
      referrer.setContent("[[OldNb:MyNote]]");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRootInNotebook(target, nb2);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[NewNb:MyNote|OldNb:MyNote]]"));
    }

    @Test
    void crossNotebookMove_preservesExplicitDisplayText() throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root1").notebook(nb1).please();
      Notebook nb2 = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root2").notebook(nb2).please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();
      referrer.setContent("[[OldNb:MyNote|custom text]]");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRootInNotebook(target, nb2);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[NewNb:MyNote|custom text]]"));
    }

    @Test
    void crossNotebookMove_doesNotRewriteWhenNotebookUnchanged()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root1").notebook(nb1).please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();
      referrer.setContent("[[MyNote]]");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRootInNotebook(target, nb1);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[MyNote]]"));
    }

    @Test
    void sameNotebookMove_doesNotRewriteReferrer() throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("SameNb").creatorAndOwner(u).please();
      makeMe.aRootNote("root").notebook(nb1).please();
      Folder folder = makeMe.aFolder().notebook(nb1).name("F").please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();
      referrer.setContent("[[MyNote]]");
      makeMe.entityPersister.flush();
      controller.moveNoteToFolder(target, folder);
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRoot(target);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[MyNote]]"));
    }
  }
}
