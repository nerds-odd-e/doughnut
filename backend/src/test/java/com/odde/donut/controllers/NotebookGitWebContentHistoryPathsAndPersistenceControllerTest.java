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
import org.junit.jupiter.api.Test;

class NotebookGitWebContentHistoryPathsAndPersistenceControllerTest
    extends NotebookGitWebContentHistoryControllerTestSupport {

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
