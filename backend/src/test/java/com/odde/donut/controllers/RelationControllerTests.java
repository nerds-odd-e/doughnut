package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.WikiTitleCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerTests extends ControllerTestBase {
  @Autowired RelationController controller;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
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
      Note mover = makeMe.aNote("M").notebookOwnedBy(currentUser.getUser()).please();
      Folder folder = makeMe.aFolder().notebook(mover.getNotebook()).name("F").please();
      controller.moveNoteToFolder(mover, folder);

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
      Note mover = makeMe.aNote("M").notebookOwnedBy(currentUser.getUser()).please();
      Folder folder = makeMe.aFolder().notebook(mover.getNotebook()).name("F").please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.moveNoteToFolder(mover, folder);

      controller.moveNoteToNotebookRootInNotebook(mover, nb2);

      makeMe.refresh(mover);
      assertThat(mover.getFolder(), nullValue());
      assertThat(mover.getNotebook().getId(), equalTo(nb2.getId()));
    }

    @Test
    void moveNoteToNotebookRootInNotebook_rejectsUnauthorizedTargetNotebook() {
      Note mover = makeMe.aNote("M").notebookOwnedBy(currentUser.getUser()).please();
      Notebook foreignNb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.moveNoteToNotebookRootInNotebook(mover, foreignNb));
    }

    @ParameterizedTest
    @CsvSource({
      "[[MyNote]], [[NewNb:MyNote|MyNote]]",
      "[[OldNb:MyNote]], [[NewNb:MyNote|OldNb:MyNote]]",
      "[[OldNb:MyNote|custom text]], [[NewNb:MyNote|custom text]]",
      "[[MyNote#prop:a%20part%20of|shown]], [[NewNb:MyNote#prop:a%20part%20of|shown]]"
    })
    void crossNotebookMove_rewritesInboundReferrerLinks(String before, String after)
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = ownedNotebook("OldNb");
      Notebook nb2 = ownedNotebook("NewNb");
      Note target =
          makeMe
              .aNote("MyNote")
              .notebook(nb1)
              .content(Frontmatter.empty().set("a part of", "v").fenced(""))
              .please();
      Note referrer = makeMe.aNote("Carrier").underSameNotebookAs(target).content(before).please();
      wikiTitleCacheService.refreshForNote(referrer, u);

      controller.moveNoteToNotebookRootInNotebook(target, nb2);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo(after));
    }

    @Test
    void crossNotebookMove_rewritesMovedNotesOutgoingUnqualifiedLinks()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = ownedNotebook("OldNb");
      Notebook nb2 = ownedNotebook("NewNb");
      Notebook nb3 = ownedNotebook("OtherNb");
      Note oldTarget = makeMe.aNote("X").notebook(nb1).please();
      makeMe.aNote("X").notebook(nb2).please();
      Note qualifiedTarget = makeMe.aNote("Y").notebook(nb3).please();
      Note mover =
          makeMe.aNote("Mover").notebook(nb1).content("See [[X]] and [[OtherNb:Y]].").please();
      wikiTitleCacheService.refreshForNote(mover, u);

      controller.moveNoteToNotebookRootInNotebook(mover, nb2);

      makeMe.refresh(mover);
      assertThat(mover.getContent(), equalTo("See [[OldNb:X|X]] and [[OtherNb:Y]]."));
      assertThat(
          wikiTitleCacheService.wikiTitlesForViewer(mover, u).stream()
              .map(wt -> wt.getNoteId())
              .toList(),
          containsInAnyOrder(oldTarget.getId(), qualifiedTarget.getId()));
    }

    @Test
    void crossNotebookMove_doesNotRewriteWhenNotebookUnchanged()
        throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = ownedNotebook("OldNb");
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer =
          makeMe.aNote("Carrier").underSameNotebookAs(target).content("[[MyNote]]").please();
      wikiTitleCacheService.refreshForNote(referrer, u);

      controller.moveNoteToNotebookRootInNotebook(target, nb1);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[MyNote]]"));
    }

    @Test
    void sameNotebookMove_doesNotRewriteReferrer() throws UnexpectedNoAccessRightException {
      User u = currentUser.getUser();
      Notebook nb1 = ownedNotebook("SameNb");
      Folder folder = makeMe.aFolder().notebook(nb1).name("F").please();
      Note target = makeMe.aNote("MyNote").notebook(nb1).please();
      Note referrer =
          makeMe.aNote("Carrier").underSameNotebookAs(target).content("[[MyNote]]").please();
      controller.moveNoteToFolder(target, folder);
      wikiTitleCacheService.refreshForNote(referrer, u);

      controller.moveNoteToNotebookRoot(target);

      makeMe.refresh(referrer);
      assertThat(referrer.getContent(), equalTo("[[MyNote]]"));
    }
  }
}
