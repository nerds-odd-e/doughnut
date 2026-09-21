package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NotebookGitFolderDissolveAtomicControllerTest extends NotebookGitControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String REFERRER_BODY_BEFORE = "See [[Outer/Biology/Cells|shown]] for details.";

  @Autowired FolderRepository folderRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackRemovedFolderPromotionReferrerAndAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder biology = makeMe.aFolder().parentFolder(outer).name("Biology").please();
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
    var acceptedHistoryBefore = acceptedHistory(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    var bindingUpdatedAt = binding.getUpdatedAt();

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> folderController.dissolveFolder(notebook, biology, false));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder reloadedBiology = folderRepository.findById(biology.getId()).orElseThrow();
          Note reloadedCells = noteRepository.findById(cells.getId()).orElseThrow();
          Note reloadedReferrer = noteRepository.findById(referrer.getId()).orElseThrow();
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedBiology.getName(), is("Biology"));
          assertThat(reloadedCells.getFolder().getId(), equalTo(biology.getId()));
          assertThat(reloadedReferrer.getContent(), is(REFERRER_BODY_BEFORE));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }
}
