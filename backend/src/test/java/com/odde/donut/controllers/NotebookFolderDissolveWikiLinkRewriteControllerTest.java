package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookFolderDissolveWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Test
  void dissolvingFolder_rewritesInboundLinksToDescendantFromInsideAndOutside()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = ownedNotebook("Nb");
    Folder outer = ownedFolder(nb, "Outer");
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    makeMe.aNote("Target").folder(mid).please();
    Note insideReferrer = makeMe.aNote("Inside").folder(mid).please();
    authorReferencingContent(insideReferrer, "[[/Outer/Mid/Target]]");
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nb).please();
    authorReferencingContent(outsideReferrer, "[[/Outer/Mid/Target]]");

    controller.dissolveFolder(nb, mid, false);

    makeMe.refresh(insideReferrer);
    makeMe.refresh(outsideReferrer);
    assertThat(insideReferrer.getContent(), equalTo("[[Outer/Target]]"));
    assertThat(outsideReferrer.getContent(), equalTo("[[Outer/Target]]"));
  }

  @Test
  void dissolveMerge_rewritesInboundLinksToPromotedSubfolderDescendant()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = ownedNotebook("Nb");
    Folder outer = ownedFolder(nb, "Outer");
    Folder outerInner = makeMe.aFolder().parentFolder(outer).name("Inner").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Folder midInner = makeMe.aFolder().parentFolder(mid).name("Inner").please();
    Note target = makeMe.aNote("Target").folder(midInner).please();
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nb).please();
    authorReferencingContent(outsideReferrer, "[[/Outer/Mid/Inner/Target]]");

    controller.dissolveFolder(nb, mid, true);

    makeMe.refresh(outsideReferrer);
    makeMe.refresh(target);
    assertThat(target.getFolder().getId(), equalTo(outerInner.getId()));
    assertThat(outsideReferrer.getContent(), equalTo("[[Outer/Inner/Target]]"));
  }
}
