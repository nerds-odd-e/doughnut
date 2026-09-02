package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookFolderReparentWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Test
  void sameNotebookFolderReparent_rewritesInboundLinksToDescendantFromInsideAndOutside()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = ownedNotebook("Nb");
    Folder newHome = ownedFolder(nb, "NewHome");
    Folder movedFolder = ownedFolder(nb, "Moved");
    Note target = makeMe.aNote("Target").folder(movedFolder).please();
    Note insideReferrer = makeMe.aNote("Inside").folder(movedFolder).please();
    authorReferencingContent(insideReferrer, "[[/Moved/Target]]");
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nb).please();
    authorReferencingContent(outsideReferrer, "[[/Moved/Target]]");

    controller.moveFolder(nb, movedFolder, folderMove(newHome.getId()));

    makeMe.refresh(insideReferrer);
    makeMe.refresh(outsideReferrer);
    assertThat(insideReferrer.getContent(), equalTo("[[NewHome/Moved/Target]]"));
    assertThat(outsideReferrer.getContent(), equalTo("[[NewHome/Moved/Target]]"));
  }
}
