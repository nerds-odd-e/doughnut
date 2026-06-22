package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerTests extends ControllerTestBase {
  @Autowired RelationController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
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

    @ParameterizedTest
    @CsvSource({
      "[[MyNote]], [[NewNb:MyNote|MyNote]]",
      "[[OldNb:MyNote]], [[NewNb:MyNote|OldNb:MyNote]]",
      "[[OldNb:MyNote|custom text]], [[NewNb:MyNote|custom text]]"
    })
    void crossNotebookMove_rewritesInboundReferrerLinks(String before, String after)
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      Notebook nb2 = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer = makeMe.aNote("Carrier").notebook(nb1).please();
      referrer.setContent(before);
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(referrer, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRootInNotebook(target, nb2);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo(after));
    }

    @Test
    void crossNotebookMove_rewritesMovedNotesOutgoingUnqualifiedLinks()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = makeMe.aNotebook().name("OldNb").creatorAndOwner(u).please();
      Notebook nb2 = makeMe.aNotebook().name("NewNb").creatorAndOwner(u).please();
      Notebook nb3 = makeMe.aNotebook().name("OtherNb").creatorAndOwner(u).please();
      Note oldTarget = makeMe.aNote("X").notebook(nb1).please();
      makeMe.aNote("X").notebook(nb2).please();
      Note qualifiedTarget = makeMe.aNote("Y").notebook(nb3).please();
      Note mover = makeMe.aNote("Mover").notebook(nb1).please();
      mover.setContent("See [[X]] and [[OtherNb:Y]].");
      makeMe.entityPersister.flush();
      wikiTitleCacheServiceBean.refreshForNote(mover, u);
      makeMe.entityPersister.flush();

      controller.moveNoteToNotebookRootInNotebook(mover, nb2);

      makeMe.refresh(mover);
      assertThat(mover.getContent(), equalTo("See [[OldNb:X|X]] and [[OtherNb:Y]]."));
      assertThat(
          wikiTitleCacheServiceBean.wikiTitlesForViewer(mover, u).stream()
              .map(wt -> wt.getNoteId())
              .toList(),
          containsInAnyOrder(oldTarget.getId(), qualifiedTarget.getId()));
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
