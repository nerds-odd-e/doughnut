package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

abstract class NotebookGitWebContentAmendmentControllerTestSupport
    extends NotebookGitWebContentControllerTestBase {

  static final Instant T1000 = Instant.parse("2026-09-08T10:00:00Z");
  static final Instant T1008 = Instant.parse("2026-09-08T10:08:00Z");
  static final Instant T100959 = Instant.parse("2026-09-08T10:09:59Z");
  static final Instant T1016 = Instant.parse("2026-09-08T10:16:00Z");
  static final Instant T1027 = Instant.parse("2026-09-08T10:27:00Z");

  Fixture fixture(String title) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title(title).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    return new Fixture(notebook.getId(), note.getId());
  }

  void saveAt(Integer noteId, String content, Instant when) throws Exception {
    testabilitySettings.timeTravelTo(Timestamp.from(when));
    saveContent(noteId, content);
  }

  NoteRealm saveContent(Integer noteId, String content) throws Exception {
    return textContentController.updateNoteContent(
        noteRepository.findById(noteId).orElseThrow(), contentDto(content));
  }

  static String content(String body) {
    return "---\ntype: Note\n---\n" + body;
  }

  NotebookGitBinding bindingById(Integer notebookId) {
    return notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
  }

  List<String> contentChain(Integer notebookId, String path) throws Exception {
    return historyFromBinding(notebookId, path).contentsNewestFirst();
  }

  History downloadHistory(Notebook notebook, String path) throws Exception {
    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    return historyFromBundle(downloaded, path);
  }

  History historyFromBinding(Integer notebookId, String path) throws Exception {
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
    return historyFromBundle(binding.getBundleBytes(), path);
  }

  static History historyFromBundle(byte[] bundleBytes, String path) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, bundleBytes);
      List<String> contents = new ArrayList<>();
      List<ObjectId> heads = new ArrayList<>();
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(tip);
        while (true) {
          heads.add(commit.getId());
          contents.add(NotebookGitProposalBlobText.readUtf8(repository, commit, path));
          if (commit.getParentCount() == 0) {
            break;
          }
          commit = revWalk.parseCommit(commit.getParent(0));
        }
      }
      return new History(bundleBytes, contents, heads);
    }
  }

  <F, S> NotebookGitConcurrentWriterTestSupport.Result<F, S> queuedWriters(
      Integer notebookId, Callable<F> firstCall, Callable<S> secondCall) throws Exception {
    return NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
        transactionManager,
        notebookGitBindingRepository,
        currentUser,
        currentUser.getUser(),
        notebookId,
        firstCall,
        secondCall);
  }

  record Fixture(Integer notebookId, Integer noteId) {}

  record History(
      byte[] bundleBytes, List<String> contentsNewestFirst, List<ObjectId> headsNewestFirst) {}
}
