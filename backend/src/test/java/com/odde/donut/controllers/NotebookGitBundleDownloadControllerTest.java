package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.concurrent.Callable;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitBundleDownloadControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String FIRST_WEB_CONTENT = "---\ntype: Note\n---\nfirst web edit";

  @Test
  void downloadReturnsTheAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String expectedHead = binding.getAcceptedGitObjectId();

    ResponseEntity<byte[]> response = controller.downloadNotebookGitBundle(notebook);

    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, response.getBody());
      assertThat(head.getName(), equalTo(expectedHead));
    }
    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(expectedHead));
  }

  @Test
  void downloadReadsTheNotebooksStoredObjectsInOneFetch() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note first =
        makeMe.aNote().notebook(notebook).title("first").content(ACCEPTED_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("second").content(ACCEPTED_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("third").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    textContentController.updateNoteContent(first, contentDto(FIRST_WEB_CONTENT));
    textContentController.updateNoteContent(first, contentDto(EDITED_CONTENT));
    String expectedHead = binding(notebook).getAcceptedGitObjectId();

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    ResponseEntity<byte[]> response;
    try (AutoCloseable ignored = callLog.activate()) {
      response = controller.downloadNotebookGitBundle(notebook);
    }

    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, response.getBody());
      assertThat(head.getName(), equalTo(expectedHead));
    }
    assertThat(callLog.countObjectFetches(), equalTo(1L));
  }

  @Test
  void downloadWaitsOnBindingLockThenReturnsCommittedCurrentHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding before = snapshotCurrentPortableTree(notebook);
    String headBefore = before.getAcceptedGitObjectId();
    Integer notebookId = notebook.getId();
    Integer noteId = note.getId();

    NotebookGitConcurrentWriterTestSupport.Result<NoteRealm, ResponseEntity<byte[]>> race =
        queuedWriters(
            notebookId,
            () ->
                textContentController.updateNoteContent(
                    noteRepository.findById(noteId).orElseThrow(), contentDto(FIRST_WEB_CONTENT)),
            () ->
                controller.downloadNotebookGitBundle(
                    notebookRepository.findById(notebookId).orElseThrow()));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), not(equalTo(headBefore)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = GitBundleTestReader.fetchHead(repository, race.second().getBody());
      assertThat(downloadedHead.getName(), equalTo(after.getAcceptedGitObjectId()));
    }
    assertThat(race.first().getNote().getContent(), is(FIRST_WEB_CONTENT));
  }

  @Test
  void deniedDownloadLeavesAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    var acceptedHistoryBefore = acceptedHistory(notebook);
    User owner = currentUser.getUser();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));

    currentUser.setUser(owner);
    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  @Test
  void missingBindingRemainsNotFound() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    notebookGitBindingRepository.delete(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.downloadNotebookGitBundle(notebook));

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    assertThat(exception.getReason(), equalTo("Notebook has no Git binding."));
  }

  private <F, S> NotebookGitConcurrentWriterTestSupport.Result<F, S> queuedWriters(
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
}
