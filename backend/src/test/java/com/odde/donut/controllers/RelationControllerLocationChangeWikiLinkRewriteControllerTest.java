package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class RelationControllerLocationChangeWikiLinkRewriteControllerTest extends ControllerTestBase {
  @Autowired RelationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @ParameterizedTest
  @CsvSource({
    "See [[/Title]]., See [[Dest/Title]].",
    "See [[/Title.md#prop:a%20part%20of|shown]]., See [[Dest/Title.md#prop:a%20part%20of|shown]]."
  })
  void sameNotebookMoveToFolder_rewritesExactRootWikiPath(String before, String after)
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook("LocNb");
    Folder dest = makeMe.aFolder().notebook(notebook).name("Dest").please();
    Note target =
        makeMe
            .aNote("Title")
            .notebook(notebook)
            .content(Frontmatter.empty().set("a part of", "v").fenced(""))
            .please();
    Note referrer = makeMe.aNote("Carrier").underSameNotebookAs(target).please();
    authorReferencingContent(referrer, before);

    controller.moveNoteToFolder(target, dest);

    makeMe.refresh(referrer);
    assertThat(referrer.getContent(), equalTo(after));
  }

  @Test
  void sameNotebookMoveToNotebookRoot_rewritesExactFolderWikiPath()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = ownedNotebook("LocNb");
    Folder source = makeMe.aFolder().notebook(notebook).name("Src").please();
    Note target = makeMe.aNote("Title").folder(source).please();
    Note referrer = makeMe.aNote("Carrier").notebook(notebook).please();
    authorReferencingContent(
        referrer, "See [[Src/Title.md|shown]] and [stay](/n" + target.getId() + ").");

    controller.moveNoteToNotebookRoot(target);

    makeMe.refresh(referrer);
    assertThat(
        referrer.getContent(),
        equalTo("See [[/Title.md|shown]] and [stay](/n" + target.getId() + ")."));
  }

  private Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
  }
}
