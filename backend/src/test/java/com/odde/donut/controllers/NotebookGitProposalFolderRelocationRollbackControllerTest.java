package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves {@code NotebookGitProposalFolderAcceptance} rolls back source parent with the rest of the
 * publish transaction when late binding acceptance fails, reusing {@link
 * NotebookGitPublicationAtomicTestSupport}'s exact failure injection and reset hook.
 */
@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitProposalFolderRelocationRollbackControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String NOTE = "---\ntype: Note\n---\nnote";

  @Autowired FolderRepository folderRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void
      lateBindingSaveFailureRollsBackAFolderMoveLeavingTheOldParentDescendantIdsAndAcceptedBinding()
          throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Folder sub = makeMe.aFolder().parentFolder(topics).name("Sub").please();
    Note nested = makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    Note deeper = makeMe.aNote().folder(sub).title("B").content(NOTE).please();
    Integer originalSourceParentId = topics.getParentFolderId();
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
            binding,
            List.of(
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/A.md", NOTE),
                new NotebookGitProposalFile("Archive/Topics/Sub/B.md", NOTE)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder reloadedSource = folderRepository.findById(topics.getId()).orElseThrow();
          assertThat(reloadedSource.getParentFolderId(), is(originalSourceParentId));
          assertThat(
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              containsInAnyOrder(archive.getId(), topics.getId(), sub.getId()));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Note::getId)
                  .toList(),
              containsInAnyOrder(nested.getId(), deeper.getId()));
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }
}
