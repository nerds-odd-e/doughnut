package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ResolvedWikiLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderMoveWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired ResolvedWikiLinkService resolvedWikiLinkServiceBean;

  @Test
  void crossNotebookFolderMove_rewritesInboundLinksFromOutsideReferrerOnly()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nbA = ownedNotebook("NbA");
    Notebook nbB = ownedNotebook("NbB");
    Folder folderF = ownedFolder(nbA, "F");
    makeMe.aNote("Target").folder(folderF).please();
    Note insideReferrer = makeMe.aNote("Inside").folder(folderF).please();
    authorReferencingContent(insideReferrer, "[[Target]]");
    Note outsideReferrer = makeMe.aNote("Outside").notebook(nbA).please();
    authorReferencingContent(outsideReferrer, "[[Target]]");
    resolvedWikiLinkServiceBean.refreshForNote(insideReferrer, owner);
    resolvedWikiLinkServiceBean.refreshForNote(outsideReferrer, owner);

    controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null));

    makeMe.refresh(outsideReferrer);
    makeMe.refresh(insideReferrer);
    assertThat(outsideReferrer.getContent(), equalTo("[[NbB:Target|Target]]"));
    assertThat(insideReferrer.getContent(), equalTo("[[Target]]"));
  }

  @Test
  void crossNotebookFolderMove_qualifiesAndLengthensInboundLinkForDestinationNamesake()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook sourceNotebook = ownedNotebook("Source");
    Notebook destinationNotebook = ownedNotebook("Destination");
    makeMe.aNote("Target").notebook(destinationNotebook).please();
    Folder movedFolder = ownedFolder(sourceNotebook, "Moved");
    makeMe.aNote("Target").folder(movedFolder).please();
    Note referrer = makeMe.aNote("Referrer").notebook(sourceNotebook).please();
    authorReferencingContent(referrer, "[[Target]]");
    resolvedWikiLinkServiceBean.refreshForNote(referrer, owner);

    controller.moveFolder(sourceNotebook, movedFolder, folderMoveTo(destinationNotebook, null));

    makeMe.refresh(referrer);
    assertThat(referrer.getContent(), equalTo("[[Destination:Moved/Target|Target]]"));
  }

  @Test
  void crossNotebookFolderMove_lengthensCoMovedPeerLinkWhenDestinationHasSameTitleNote()
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
    resolvedWikiLinkServiceBean.refreshForNote(insideNote, owner);

    controller.moveFolder(nbA, folderF, folderMoveTo(nbB, null));

    makeMe.refresh(insideNote);
    assertThat(insideNote.getContent(), equalTo("[[NbA:Outside|Outside]] and [[F/Peer|Peer]]."));
  }

  @Test
  void crossNotebookFolderMove_keepsPathShapedLinksToCoMovedNotes()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook oldNb = ownedNotebook("OldNb");
    Notebook newNb = ownedNotebook("NewNb");
    Folder folderF = ownedFolder(oldNb, "F");
    Note noteA = makeMe.aNote("A").folder(folderF).please();
    Note noteB = makeMe.aNote("B").folder(folderF).content("[[F/A]] and [label](/F/A.md)").please();
    resolvedWikiLinkServiceBean.refreshForNote(noteB, owner);

    controller.moveFolder(oldNb, folderF, folderMoveTo(newNb, null));

    makeMe.refresh(noteB);
    assertThat(noteB.getContent(), equalTo("[[F/A]] and [label](/F/A.md)"));
    assertThat(
        resolvedWikiLinkServiceBean.wikiLinksForViewer(noteB, owner).stream()
            .map(WikiLink::getDestinationNoteId)
            .toList(),
        containsInAnyOrder(noteA.getId()));
  }
}
