package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiLinkRewriteServiceFolderNotebookMoveTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiLinkRewriteService wikiLinkRewriteService;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  @Test
  void rewriteOutgoingWikiLinksForFolderNotebookMove_qualifiesOutsideTargetsOnly() {
    User owner = makeMe.aUser().please();
    Notebook sourceNotebook = makeMe.aNotebook().name("NbA").creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(sourceNotebook).name("F").please();
    Note insideNote = makeMe.aNote("Inside").folder(folder).please();
    Note peer = makeMe.aNote("Peer").folder(folder).please();
    makeMe.aNote("Outside").notebook(sourceNotebook).please();
    insideNote.setContent("[[Outside]] and [[Peer]].");
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(insideNote, owner);
    makeMe.entityPersister.flush();

    Timestamp now = makeMe.aTimestamp().please();
    wikiLinkRewriteService.rewriteOutgoingWikiLinksForFolderNotebookMove(
        Set.of(insideNote.getId(), peer.getId()), sourceNotebook.getName(), now, owner);

    makeMe.refresh(insideNote);
    assertThat(insideNote.getContent(), equalTo("[[NbA:Outside|Outside]] and [[Peer]]."));
  }
}
