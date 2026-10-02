package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.NumberedPropertyMigration;
import com.odde.donut.services.WikiLinkResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationVisibilityControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;
  @Autowired WikiLinkResolver resolver;

  @Test
  void privateSameNameCandidatePreservesOwnerAnonymousAndWiderReaderResults() throws Exception {
    User owner = currentUser.getUser();
    User other = inCommittedTransaction(transactionManager, this::createFixtureUser);
    currentUser.setUser(other);
    Notebook hidden;
    try {
      hidden = createGitBackedNotebook("Shared Notebook");
      makeMe
          .aNote()
          .notebook(hidden)
          .title("Target")
          .content("---\ntype: Note\ntopic 2: secret\n---\nprivate")
          .please();
      snapshotCurrentPortableTree(hidden);
    } finally {
      currentUser.setUser(owner);
    }
    Notebook notebook = createGitBackedNotebook("Shared Notebook");
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Target")
            .content("---\ntype: Note\ntopic 2: A\n---\npublic")
            .please();
    Note source =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Source")
            .content("See [[Shared Notebook:Target#prop:topic%202|detail]].")
            .please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aBazaarNotebook(notebookRepository.findById(notebook.getId()).orElseThrow())
                .please());
    snapshotCurrentPortableTree(notebook);
    assertReaderResults(source, target, other, "Shared Notebook:Target#prop:topic%202|detail");
    currentUser.setUser(other);
    var privateBefore = acceptedHistory(hidden);
    currentUser.setUser(owner);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    assertReaderResults(source, target, other, "Shared Notebook:Target#prop:topic|detail");
    String rewritten = "See [[Shared Notebook:Target#prop:topic|detail]].";
    assertThat(noteController.showNote(source).getNote().getContent(), equalTo(rewritten));
    assertThat(tipText(acceptedHistory(notebook), "Source.md"), equalTo(rewritten));
    currentUser.setUser(other);
    try {
      assertThat(acceptedHistory(hidden), equalTo(privateBefore));
    } finally {
      currentUser.setUser(owner);
    }
  }

  private void assertReaderResults(Note source, Note target, User other, String token)
      throws Exception {
    var link = noteController.showNote(source).getWikiLinks().getFirst();
    assertThat(link.getDestinationNoteId(), equalTo(target.getId()));
    assertThat(link.getTarget(), equalTo(token.substring(0, token.indexOf('|'))));
    assertThat(link.getDisplayText(), equalTo("detail"));
    var anonymous = resolver.classifyToken(token, source, null);
    assertThat(anonymous, instanceOf(WikiLinkResolver.CandidateCardinality.Resolved.class));
    assertThat(
        ((WikiLinkResolver.CandidateCardinality.Resolved) anonymous).destinationNote().getId(),
        equalTo(target.getId()));
    User owner = currentUser.getUser();
    currentUser.setUser(other);
    try {
      assertThat(
          noteController.showNote(source).getWikiLinks().getFirst().getResolution(),
          equalTo(WikiLink.Resolution.AMBIGUOUS));
    } finally {
      currentUser.setUser(owner);
    }
  }
}
