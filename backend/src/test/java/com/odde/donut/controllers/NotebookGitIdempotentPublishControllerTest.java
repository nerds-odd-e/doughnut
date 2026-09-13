package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class NotebookGitIdempotentPublishControllerTest extends NotebookGitWebContentControllerTestBase {

  @Test
  void idempotentPublishReturnsUnchangedHeadAndHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String expectedHead = binding.getAcceptedGitObjectId();
    byte[] currentBundle = binding.getBundleBytes();

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), expectedHead, currentBundle);

    assertThat(publishedHead, equalTo(expectedHead));
    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(expectedHead));
    assertThat(after.getBundleBytes(), equalTo(currentBundle));
  }

  @Test
  void rejectedPublishLeavesAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    byte[] currentBundle = binding.getBundleBytes();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, currentBundle));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(after.getBundleBytes(), equalTo(currentBundle));
  }
}
