package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitDiagnosis;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitDiagnosisControllerTest extends NotebookGitControllerTestBase {
  @Autowired TextContentController textContentController;

  @Test
  void reportsMatchingNativeTreesAtTheAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(NOTE).please();
    var binding = snapshotCurrentPortableTree(notebook);

    var report = controller.diagnoseNotebookGit(notebook.getId());

    assertThat(report.acceptedHead(), equalTo(binding.getAcceptedGitObjectId()));
    assertThat(report.acceptedTree(), equalTo(acceptedTreeId(notebook)));
    assertThat(report.liveTree(), equalTo(report.acceptedTree()));
    assertThat(report.differences(), equalTo(List.of()));
  }

  @Test
  void reportsSortedExactContentForChangedAddedAndRemovedPortableFiles() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note changed = makeMe.aNote().notebook(notebook).title("changed").content(NOTE).please();
    Note removed = makeMe.aNote().notebook(notebook).title("removed").content(NOTE).please();
    var empty = makeMe.aFolder().notebook(notebook).name("empty").please();
    NotebookAttachment attachment =
        makeMe.anAttachment("file.bin").atRootOf(notebook).content(new byte[] {0, 1, -1}).please();
    byte[] oldPointer = attachment.getAcceptedGitContent();
    inCommittedTransaction(
        transactionManager,
        () ->
            notebookRepository.findById(notebook.getId()).orElseThrow().setReadmeContent("before"));
    var binding =
        snapshotCurrentPortableTree(notebookRepository.findById(notebook.getId()).orElseThrow());
    String oldTree = acceptedTreeId(notebook);
    byte[] newPointer = pointerFor(notebook, new byte[] {-1, 0, 2});
    makeMe.aNote().notebook(notebook).title("added").content(NOTE).please();
    makeMe.aNote().folder(empty).title("new").content(NOTE).please();
    makeMe.aFolder().notebook(notebook).name("new-empty").please();

    inCommittedTransaction(
        transactionManager,
        () -> {
          noteRepository.findById(changed.getId()).orElseThrow().setContent(NOTE + " changed");
          entityManager.remove(noteRepository.findById(removed.getId()).orElseThrow());
          notebookRepository.findById(notebook.getId()).orElseThrow().setReadmeContent("after");
          entityManager
              .find(NotebookAttachment.class, attachment.getId())
              .setAcceptedGitContent(newPointer);
        });

    var report = controller.diagnoseNotebookGit(notebook.getId());

    assertThat(report.acceptedHead(), equalTo(binding.getAcceptedGitObjectId()));
    assertThat(report.acceptedTree(), equalTo(oldTree));
    assertThat(
        report.differences().stream().map(NotebookGitDiagnosis.Difference::path).toList(),
        equalTo(
            List.of(
                "README.md",
                "added.md",
                "changed.md",
                "empty/.keep",
                "empty/new.md",
                "file.bin",
                "new-empty/.keep",
                "removed.md")));
    assertDifference(
        report,
        "README.md",
        bytes("---\ntype: Readme\n---\nbefore"),
        bytes("---\ntype: Readme\n---\nafter"));
    assertDifference(report, "added.md", null, bytes(NOTE));
    assertDifference(report, "changed.md", bytes(NOTE), bytes(NOTE + " changed"));
    assertDifference(report, "empty/.keep", new byte[0], null);
    assertDifference(report, "empty/new.md", null, bytes(NOTE));
    assertDifference(report, "file.bin", oldPointer, newPointer);
    assertDifference(report, "new-empty/.keep", null, new byte[0]);
    assertDifference(report, "removed.md", bytes(NOTE), null);
    var liveEntries = new HashMap<String, byte[]>();
    GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook))
        .exactTree()
        .forEach(entry -> liveEntries.put(entry.path(), entry.content()));
    report
        .differences()
        .forEach(difference -> {
          if (difference.liveContent() == null) liveEntries.remove(difference.path());
          else liveEntries.put(difference.path(), difference.liveContent());
        });
    assertThat(report.liveTree(), equalTo(nativeTreeId(liveEntries)));
  }

  @Test
  void refusesDiagnosisForANonOwner() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.diagnoseNotebookGit(notebook.getId()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void racesWebPublicationAndReportsOneCompleteCommittedSnapshot(boolean diagnosisFirst)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(NOTE).please();
    var before = snapshotCurrentPortableTree(notebook);
    byte[] beforeBundle = acceptedBundleBytes(notebook);
    makeMe.aNote().notebook(notebook).title("unpublished").content(NOTE + " live-only").please();
    Callable<NotebookGitDiagnosis.Report> diagnose =
        () -> controller.diagnoseNotebookGit(notebook.getId());
    Callable<String> web =
        () -> {
          NoteUpdateContentDTO update = new NoteUpdateContentDTO();
          update.setContent(NOTE + " web");
          textContentController.updateNoteContent(
              noteRepository.findById(note.getId()).orElseThrow(), update);
          return reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId();
        };
    NotebookGitDiagnosis.Report report;
    String webHead;
    if (diagnosisFirst) {
      var result =
          NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
              transactionManager,
              notebookGitBindingRepository,
              currentUser,
              currentUser.getUser(),
              notebook.getId(),
              diagnose,
              web);
      report = result.first();
      webHead = result.second();
    } else {
      var result =
          NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
              transactionManager,
              notebookGitBindingRepository,
              currentUser,
              currentUser.getUser(),
              notebook.getId(),
              web,
              diagnose);
      webHead = result.first();
      report = result.second();
    }
    var observed =
        GitBundleTestReader.fetchAcceptedTip(
            diagnosisFirst ? beforeBundle : acceptedBundleBytes(notebook));
    Map<String, byte[]> expected = new HashMap<>();
    observed.exactTree().forEach(entry -> expected.put(entry.path(), entry.content()));
    assertThat(
        report.acceptedHead(), equalTo(diagnosisFirst ? before.getAcceptedGitObjectId() : webHead));
    assertThat(report.acceptedTree(), equalTo(nativeTreeId(expected)));
    assertThat(
        report.differences().stream().map(NotebookGitDiagnosis.Difference::path).toList(),
        equalTo(List.of("unpublished.md")));
    assertDifference(report, "unpublished.md", null, bytes(NOTE + " live-only"));
    expected.put("unpublished.md", bytes(NOTE + " live-only"));
    assertThat(report.liveTree(), equalTo(nativeTreeId(expected)));
  }

  private void assertDifference(
      NotebookGitDiagnosis.Report report, String path, byte[] before, byte[] after) {
    var difference =
        report.differences().stream()
            .filter(item -> item.path().equals(path))
            .findFirst()
            .orElseThrow();
    assertThat(difference.acceptedContent(), equalTo(before));
    assertThat(difference.liveContent(), equalTo(after));
    assertThat(difference.acceptedBlobId(), before == null ? nullValue() : equalTo(blobId(before)));
    assertThat(difference.liveBlobId(), after == null ? nullValue() : equalTo(blobId(after)));
  }

  private String acceptedTreeId(Notebook notebook) throws Exception {
    try (var repository = new InMemoryRepository(new DfsRepositoryDescription());
        var walk = new RevWalk(repository)) {
      return walk.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getTree()
          .name();
    }
  }

  private String nativeTreeId(Map<String, byte[]> entries) throws Exception {
    try (var repository = new InMemoryRepository(new DfsRepositoryDescription());
        var walk = new RevWalk(repository)) {
      var files =
          entries.entrySet().stream()
              .map(entry -> new NotebookGitProposalFile(entry.getKey(), entry.getValue()))
              .toList();
      return walk.parseCommit(commitOnTopOf(repository, List.of(), files, "Expected tree"))
          .getTree()
          .name();
    }
  }

  private static byte[] bytes(String content) {
    return content.getBytes(StandardCharsets.UTF_8);
  }

  private static String blobId(byte[] content) {
    return new ObjectInserter.Formatter().idFor(Constants.OBJ_BLOB, content).name();
  }
}
