package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderMoveLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  @Test
  void crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().name("NbB").creatorAndOwner(owner).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
    Note target = makeMe.aNote("Target").folder(folderF).please();
    Note insideReferrer = makeMe.aNote("Inside").folder(folderF).please();
    insideReferrer.setContent("[[Target]]");
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nbA).please();
    outsideReferrer.setContent("[[Target]]");
    makeMe.entityPersister.flush();
    wikiTitleCacheServiceBean.refreshForNote(insideReferrer, owner);
    wikiTitleCacheServiceBean.refreshForNote(outsideReferrer, owner);
    makeMe.entityPersister.flush();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    controller.moveFolder(nbA, folderF, req);

    makeMe.refresh(outsideReferrer);
    makeMe.refresh(insideReferrer);
    assertThat(outsideReferrer.getContent(), equalTo("[[NbB:Target|Target]]"));
    assertThat(insideReferrer.getContent(), equalTo("[[Target]]"));
  }

  @Test
  void crossNotebookFolderMove_keepsCoMovedPeerLinkRelativeWhenDestinationHasSameTitleNote()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
    Notebook nbB = makeMe.aNotebook().name("NbB").creatorAndOwner(owner).please();
    makeMe.aNote("Peer").notebook(nbB).please();
    Folder folderF = makeMe.aFolder().notebook(nbA).name("F").please();
    Note insideNote = makeMe.aNote("Inside").folder(folderF).please();
    makeMe.aNote("Peer").folder(folderF).please();
    makeMe.aNote("Outside").notebook(nbA).please();
    insideNote.setContent("[[Outside]] and [[Peer]].");
    makeMe.entityPersister.flush();
    wikiTitleCacheServiceBean.refreshForNote(insideNote, owner);
    makeMe.entityPersister.flush();

    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(nbB.getId());
    controller.moveFolder(nbA, folderF, req);

    makeMe.refresh(insideNote);
    assertThat(insideNote.getContent(), equalTo("[[NbA:Outside|Outside]] and [[Peer]]."));
  }
}
