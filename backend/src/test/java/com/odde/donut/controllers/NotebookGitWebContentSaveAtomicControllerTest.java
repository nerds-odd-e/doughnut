package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that a web note-content save failing after note projection, at the accepted binding
 * save, leaves the note, its references and the accepted Git state unchanged.
 */
@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NotebookGitWebContentSaveAtomicControllerTest extends NotebookGitControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\n[[new reference]]";

  @Autowired TextContentController textContentController;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  /** Triggers {@link TextContentController#updateNoteContent} and asserts the injected failure. */
  private void triggerFailingContentUpdate(Note note, String proposedContent) {
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent(proposedContent);

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class, () -> textContentController.updateNoteContent(note, update));
    assertThat(failure.getMessage(), is("forced failure after note projection"));
  }

  @Test
  void lateBindingSaveFailureRollsBackWebContentTimestampReferencesAndAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    var acceptedHistoryBefore = acceptedHistory(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    Timestamp noteUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(note.getId()).orElseThrow().getUpdatedAt());
    triggerFailingContentUpdate(note, PROPOSED_CONTENT);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedNote.getContent(), is(ACCEPTED_CONTENT));
          assertThat(reloadedNote.getUpdatedAt(), is(noteUpdatedAt));
          assertThat(rowsFor(entityManager, reloadedNote), empty());
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  /**
   * The append that runs before the injected failure already flushes the new blob/tree/commit rows
   * into {@code notebook_git_accepted_object} on the same, still-open business transaction; only
   * the later {@code entityPersister.save(binding)} call fails. Ordinary test-managed rollback
   * cannot distinguish "never written" from "written then rolled back", so this proves the native
   * rows specifically from a separately committed reader and a freshly reopened repository (no
   * process-local cache): failure after native object insertion leaves the previously accepted
   * state completely unchanged, not a durable head advanced past missing objects and not leftover
   * garbage rows either.
   */
  @Test
  void lateBindingSaveFailureLeavesNoDurableNativeObjectStoreRows() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding acceptedBinding = snapshotCurrentPortableTree(notebook);
    Integer bindingId = acceptedBinding.getId();
    String acceptedHead = acceptedBinding.getAcceptedGitObjectId();
    long nativeRowsBeforeFailedSave = countNativeObjectStoreRows(bindingId);

    triggerFailingContentUpdate(note, PROPOSED_CONTENT);

    assertThat(countNativeObjectStoreRows(bindingId), is(nativeRowsBeforeFailedSave));
    NotebookGitBinding reloadedBinding = reloadCommittedBinding(notebook.getId());
    assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));

    byte[] reopenedBundle = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, reopenedBundle);
      assertThat(head.getName(), is(acceptedHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, head, "note.md"), is(ACCEPTED_CONTENT));
    }
  }
}
