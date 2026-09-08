package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.NotebookGitBindingAmendmentFixture.markEligible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import java.time.Instant;
import java.util.concurrent.Callable;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitBundleDownloadFreezeControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final Instant ELIGIBLE_AT = Instant.parse("2026-09-08T10:00:00Z");
  private static final String FIRST_WEB_CONTENT = "---\ntype: Note\n---\nfirst web edit";

  @Test
  void downloadFreezesEligibleTipAndReturnsThatHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    markEligible(binding, note.getId(), ELIGIBLE_AT);
    notebookGitBindingRepository.save(binding);
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
    assertThat(after.getAmendmentHead(), nullValue());
    assertThat(after.getAmendmentNoteId(), nullValue());
    assertThat(after.getAmendmentLastChangedAt(), nullValue());
  }

  @Test
  void downloadWaitsOnBindingLockThenReturnsCommittedCurrentHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding before = snapshotCurrentPortableTree(notebook);
    markEligible(before, note.getId(), ELIGIBLE_AT);
    notebookGitBindingRepository.save(before);
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
    assertThat(after.getAmendmentHead(), nullValue());
    assertThat(race.first().getNote().getContent(), is(FIRST_WEB_CONTENT));
  }

  @Test
  void deniedDownloadLeavesEligibilityUntouched() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    markEligible(binding, note.getId(), ELIGIBLE_AT);
    notebookGitBindingRepository.save(binding);
    String eligibleHead = binding.getAcceptedGitObjectId();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadNotebookGitBundle(notebook));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAmendmentHead(), equalTo(eligibleHead));
    assertThat(after.getAmendmentNoteId(), equalTo(note.getId()));
    assertThat(after.getAmendmentLastChangedAt().toInstant(), equalTo(ELIGIBLE_AT));
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
