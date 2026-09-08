package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.NotebookGitBindingAmendmentFixture.markEligible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotebookGitIdempotentPublishFreezeControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-08T10:00:00Z");

  @Test
  void idempotentPublishFreezesEligibleTipAndReturnsUnchangedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    markEligible(binding, note.getId(), ELIGIBLE_AT);
    notebookGitBindingRepository.save(binding);
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
    assertThat(after.getAmendmentHead(), nullValue());
    assertThat(after.getAmendmentNoteId(), nullValue());
    assertThat(after.getAmendmentLastChangedAt(), nullValue());
  }

  @Test
  void rejectedPublishLeavesEligibilityUntouched() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    markEligible(binding, note.getId(), ELIGIBLE_AT);
    notebookGitBindingRepository.save(binding);
    String eligibleHead = binding.getAcceptedGitObjectId();
    byte[] currentBundle = binding.getBundleBytes();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.publishNotebookGitProposal(notebook.getId(), eligibleHead, currentBundle));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAmendmentHead(), equalTo(eligibleHead));
    assertThat(after.getAmendmentNoteId(), equalTo(note.getId()));
    assertThat(after.getAmendmentLastChangedAt().toInstant(), equalTo(ELIGIBLE_AT));
  }
}
