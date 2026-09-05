package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class NotebookGitCommittedProposalControllerTestBase
    extends NotebookGitBundleControllerTestBase {

  private static final String FIXTURE_PREFIX = "notebook-git-proposal-committed-";
  private static final long WORKER_TIMEOUT_SECONDS = 10;
  static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nproposed content";

  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @BeforeEach
  void replaceRollbackFixtureWithCommittedUser() {
    String setupUserExternalIdentifier = currentUser.getUser().getExternalIdentifier();
    committed(() -> deleteByUserExternalIdentifierLike(entityManager, setupUserExternalIdentifier));
    committed(() -> currentUser.setUser(makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please()));
  }

  @AfterEach
  void cleanupCommittedFixture() {
    committed(() -> deleteByUserExternalIdentifierLike(entityManager, FIXTURE_PREFIX + "%"));
  }

  CommittedProposalFixture createCommittedProposalFixture() throws Exception {
    ProposalSeed seed =
        committed(
            () -> {
              Notebook notebook = createGitBackedNotebookUnchecked();
              Note note =
                  makeMe
                      .aNote()
                      .notebook(notebook)
                      .title("note")
                      .content(ACCEPTED_CONTENT)
                      .please();
              snapshotCurrentPortableTree(notebook);
              return new ProposalSeed(notebook.getId(), note.getId());
            });

    NotebookGitBinding binding = reloadBinding(seed.notebookId());
    byte[] proposal =
        proposalBundleBytes(binding, List.of(new ProposedFile("note.md", PROPOSED_CONTENT)));
    return new CommittedProposalFixture(
        seed.notebookId(), seed.noteId(), binding.getAcceptedGitObjectId(), proposal);
  }

  private Notebook createGitBackedNotebookUnchecked() {
    try {
      return createGitBackedNotebook();
    } catch (UnexpectedNoAccessRightException exception) {
      throw new AssertionError(exception);
    }
  }

  Notebook reloadNotebook(Integer notebookId) {
    return committed(() -> notebookRepository.findById(notebookId).orElseThrow());
  }

  NotebookGitBinding reloadBinding(Integer notebookId) {
    return committed(
        () -> notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow());
  }

  <T> T inRequestContextWorker(Callable<T> action) throws Exception {
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<T> future =
          executor.submit(
              () -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                try {
                  return action.call();
                } finally {
                  RequestContextHolder.resetRequestAttributes();
                }
              });
      return future.get(WORKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (ExecutionException exception) {
      if (exception.getCause() instanceof Exception cause) {
        throw cause;
      }
      if (exception.getCause() instanceof Error cause) {
        throw cause;
      }
      throw new AssertionError(exception.getCause());
    } finally {
      executor.shutdownNow();
    }
  }

  <T> T committed(java.util.function.Supplier<T> action) {
    return inCommittedTransaction(transactionManager, action);
  }

  void committed(Runnable action) {
    inCommittedTransaction(transactionManager, action);
  }

  record CommittedProposalFixture(
      Integer notebookId, Integer noteId, String expectedHead, byte[] proposal) {}

  private record ProposalSeed(Integer notebookId, Integer noteId) {}
}
