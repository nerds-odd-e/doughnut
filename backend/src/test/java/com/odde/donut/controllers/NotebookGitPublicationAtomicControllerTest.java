package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitPublicationAtomicControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\n[[new reference]]";

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackProjectedNoteAndAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    byte[] acceptedBundle = binding.getBundleBytes();
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("note.md", PROPOSED_CONTENT)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedNote.getContent(), is(ACCEPTED_CONTENT));
          assertThat(rowsFor(entityManager, reloadedNote), empty());
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }
}
