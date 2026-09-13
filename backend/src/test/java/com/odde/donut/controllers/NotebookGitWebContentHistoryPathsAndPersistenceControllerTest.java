package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NotebookGitWebContentHistoryPathsAndPersistenceControllerTest
    extends NotebookGitWebContentHistoryControllerTestSupport {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void legacyAmendmentMetadataDoesNotChangeTheAcceptedParentInAFreshPersistenceContext()
      throws Exception {
    Fixture fixture = fixture("Fresh");
    String acceptedHeadBeforeSave =
        inCommittedTransaction(
            transactionManager,
            () -> {
              NotebookGitBinding binding = bindingById(fixture.notebookId());
              String acceptedHead = binding.getAcceptedGitObjectId();
              jdbcTemplate.update(
                  """
                  UPDATE notebook_git_binding
                  SET amendment_head = ?,
                      amendment_note_id = ?,
                      amendment_last_changed_at = ?
                  WHERE notebook_id = ?
                  """,
                  acceptedHead,
                  fixture.noteId(),
                  Timestamp.from(T1000),
                  fixture.notebookId());
              entityManager.clear();
              return acceptedHead;
            });

    saveAt(fixture.noteId(), content("changed"), T1008);

    History history = historyFromBinding(fixture.notebookId(), "Fresh.md");
    assertThat(
        history.headsNewestFirst().get(1), equalTo(ObjectId.fromString(acceptedHeadBeforeSave)));
  }

  @Test
  void rejectedSaveLeavesNoteAndAcceptedHistoryUnchanged() throws Exception {
    Fixture fixture = fixture("Reject");
    saveAt(fixture.noteId(), content("eligible"), T1000);
    NotebookGitBinding before =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    String head = before.getAcceptedGitObjectId();
    byte[] bundle = before.getBundleBytes();
    User owner = currentUser.getUser();

    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            textContentController.updateNoteContent(
                noteRepository.findById(fixture.noteId()).orElseThrow(),
                contentDto(content("denied"))));
    currentUser.setUser(owner);
    assertThrows(
        ApiException.class,
        () ->
            textContentController.updateNoteContent(
                noteRepository.findById(fixture.noteId()).orElseThrow(),
                contentDto("---\ntype: Note\naliases: invalid\n---\nbody")));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () ->
                notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(head));
    assertThat(after.getBundleBytes(), equalTo(bundle));
    assertThat(
        noteRepository.findById(fixture.noteId()).orElseThrow().getContent(),
        is(content("eligible")));
  }
}
