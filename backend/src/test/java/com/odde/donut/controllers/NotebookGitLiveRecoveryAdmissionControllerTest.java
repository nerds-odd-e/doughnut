package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitLiveRecoveryAdmissionControllerTest extends NotebookGitControllerTestBase {
  @ParameterizedTest
  @ValueSource(strings = {"untyped", "---\ntype: Note\naliases: 7\n---\nbody"})
  void refusesInvalidLiveContentWithoutAdvancingHistory(String content) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("invalid").content(content).please();
    var report = controller.diagnoseNotebookGit(notebook.getId());
    assertThrows(
        RuntimeException.class,
        () ->
            controller.recoverNotebookGit(
                notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(report.acceptedHead()));
  }

  @Test
  void refusesAmbiguousRemovalAndAddition() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    var original =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("original")
            .content(NOTE + " original distinct concept")
            .please();
    snapshotCurrentPortableTree(notebook);
    inCommittedTransaction(
        transactionManager,
        () -> entityManager.remove(noteRepository.findById(original.getId()).orElseThrow()));
    makeMe
        .aNote()
        .notebook(notebook)
        .title("unrelated")
        .content("---\ntype: Note\n---\ncompletely different")
        .please();
    var report = controller.diagnoseNotebookGit(notebook.getId());
    var failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.recoverNotebookGit(
                    notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));
    assertThat(failure.getReason(), containsString("identity correspondence is uncertain"));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(report.acceptedHead()));
  }

  @Test
  void enforcesAttachmentDurabilityForTheRecoveryCandidate() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    var attachment =
        makeMe.anAttachment("missing.bin").atRootOf(notebook).content(new byte[] {1}).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            entityManager
                .find(NotebookAttachment.class, attachment.getId())
                .setAcceptedGitContent(
                    "version https://git-lfs.github.com/spec/v1\noid sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nsize 3\n"
                        .getBytes(StandardCharsets.UTF_8)));
    var report = controller.diagnoseNotebookGit(notebook.getId());
    var failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.recoverNotebookGit(
                    notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));
    assertThat(failure.getReason(), containsString("missing"));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(),
        equalTo(report.acceptedHead()));
  }
}
