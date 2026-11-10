package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NotebookGitLiveRecoveryAtomicControllerTest extends NotebookGitControllerTestBase {
  @Autowired NotePropertyIndexRepository propertyIndexes;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateFailureRollsBackNativeObjectsHeadAndRefreshedDerivedState() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    var note = makeMe.aNote().notebook(notebook).title("note").content(NOTE).please();
    var binding = snapshotCurrentPortableTree(notebook);
    inCommittedTransaction(
        transactionManager,
        () ->
            noteRepository
                .findById(note.getId())
                .orElseThrow()
                .setContent(NotebookGitLiveRecoveryControllerTest.LIVE));
    var report = controller.diagnoseNotebookGit(notebook.getId());
    var before = acceptedHistory(notebook);
    long objectCount = countNativeObjectStoreRows(binding.getId());
    var indexBefore =
        propertyIndexes.findByNote_IdAndPropertyKeyAndPropertyValue(note.getId(), "topic", "");
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    var failure =
        assertThrows(
            RuntimeException.class,
            () ->
                controller.recoverNotebookGit(
                    notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));

    assertThat(failure.getMessage(), equalTo("forced failure after note projection"));
    assertThat(countNativeObjectStoreRows(binding.getId()), equalTo(objectCount));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(report.acceptedHead()));
    assertThat(acceptedHistory(notebook), equalTo(before));
    assertThat(
        propertyIndexes.findByNote_IdAndPropertyKeyAndPropertyValue(note.getId(), "topic", ""),
        equalTo(indexBefore));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(NotebookGitLiveRecoveryControllerTest.LIVE));
  }
}
