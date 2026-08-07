package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderMoveWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  @Test
  void crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = ownedNotebook("NbA");
    Notebook nbB = ownedNotebook("NbB");
    Folder folderF = ownedFolder(nbA, "F");
    makeMe.aNote("Target").folder(folderF).please();
    Note insideReferrer = makeMe.aNote("Inside").folder(folderF).content("[[Target]]").please();
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nbA).content("[[Target]]").please();
    wikiTitleCacheServiceBean.refreshForNote(insideReferrer, owner);
    wikiTitleCacheServiceBean.refreshForNote(outsideReferrer, owner);

    controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null));

    makeMe.refresh(outsideReferrer);
    makeMe.refresh(insideReferrer);
    assertThat(outsideReferrer.getContent(), equalTo("[[NbB:Target|Target]]"));
    assertThat(insideReferrer.getContent(), equalTo("[[Target]]"));
  }

  @Test
  void crossNotebookFolderMove_keepsCoMovedPeerLinkRelativeWhenDestinationHasSameTitleNote()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = ownedNotebook("NbA");
    Notebook nbB = ownedNotebook("NbB");
    makeMe.aNote("Peer").notebook(nbB).please();
    Folder folderF = ownedFolder(nbA, "F");
    Note insideNote =
        makeMe.aNote("Inside").folder(folderF).content("[[Outside]] and [[Peer]].").please();
    makeMe.aNote("Peer").folder(folderF).please();
    makeMe.aNote("Outside").notebook(nbA).please();
    wikiTitleCacheServiceBean.refreshForNote(insideNote, owner);

    controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null));

    makeMe.refresh(insideNote);
    assertThat(insideNote.getContent(), equalTo("[[NbA:Outside|Outside]] and [[Peer]]."));
  }
}
