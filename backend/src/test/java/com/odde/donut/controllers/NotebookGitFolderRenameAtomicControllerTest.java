package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitFolderRenameAtomicControllerTest extends NotebookGitBundleControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String REFERRER_BODY_BEFORE = "See [[Biology/Cells|shown]] for details.";

  @Autowired FolderRepository folderRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackFolderNameReferrerAndAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_BODY_BEFORE));
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    byte[] acceptedBundle = binding.getBundleBytes();
    String acceptedHead = binding.getAcceptedGitObjectId();
    var bindingUpdatedAt = binding.getUpdatedAt();
    FolderRenameRequest request = new FolderRenameRequest();
    request.setName("Zoology");

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> folderController.renameFolder(notebook, biology, request));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder reloadedBiology = folderRepository.findById(biology.getId()).orElseThrow();
          Note reloadedReferrer = noteRepository.findById(referrer.getId()).orElseThrow();
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedBiology.getName(), is("Biology"));
          assertThat(reloadedReferrer.getContent(), is(REFERRER_BODY_BEFORE));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }
}
