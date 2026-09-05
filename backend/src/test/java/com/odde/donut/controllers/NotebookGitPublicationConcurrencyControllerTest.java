package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

/** Verifies that publications competing from one accepted head cannot overwrite each other. */
class NotebookGitPublicationConcurrencyControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String FIRST_CONTENT = "---\ntype: Note\n---\nfirst publication";
  private static final String SECOND_CONTENT = "---\ntype: Note\n---\nsecond publication";

  @Test
  void competingDirectChildrenProduceOneAcceptedWinner() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Proposal first = proposal(binding, FIRST_CONTENT);
    Proposal second = proposal(binding, SECOND_CONTENT);
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    CountDownLatch bindingLocked = new CountDownLatch(1);
    CountDownLatch releaseBinding = new CountDownLatch(1);
    CyclicBarrier startTogether = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(3);

    try {
      Future<?> lockHolder =
          executor.submit(
              () -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transaction.executeWithoutResult(
                    ignored -> {
                      notebookGitBindingRepository
                          .findByNotebookIdForUpdate(notebook.getId())
                          .orElseThrow();
                      bindingLocked.countDown();
                      await(releaseBinding);
                    });
              });
      await(bindingLocked);
      Future<PublicationAttempt> firstAttempt =
          executor.submit(
              publishAfterBarrier(requestAttributes, startTogether, notebook, binding, first));
      Future<PublicationAttempt> secondAttempt =
          executor.submit(
              publishAfterBarrier(requestAttributes, startTogether, notebook, binding, second));

      assertThrows(TimeoutException.class, () -> firstAttempt.get(250, TimeUnit.MILLISECONDS));
      assertThrows(TimeoutException.class, () -> secondAttempt.get(250, TimeUnit.MILLISECONDS));
      releaseBinding.countDown();
      lockHolder.get(10, TimeUnit.SECONDS);

      List<PublicationAttempt> attempts =
          List.of(firstAttempt.get(10, TimeUnit.SECONDS), secondAttempt.get(10, TimeUnit.SECONDS));
      List<PublicationAttempt> accepted =
          attempts.stream().filter(attempt -> attempt.acceptedHead() != null).toList();
      List<PublicationAttempt> rejected =
          attempts.stream().filter(attempt -> attempt.rejection() != null).toList();

      assertThat(accepted, hasSize(1));
      assertThat(rejected, hasSize(1));
      assertThat(rejected.getFirst().rejection().getStatusCode(), equalTo(HttpStatus.CONFLICT));
      assertThat(
          rejected.getFirst().rejection().getReason(),
          containsString("expectedHead no longer matches"));

      Proposal winner =
          List.of(first, second).stream()
              .filter(
                  proposal -> proposal.head().getName().equals(accepted.getFirst().acceptedHead()))
              .findFirst()
              .orElseThrow();
      PublishedState publishedState =
          inCommittedTransaction(
              transactionManager,
              () -> {
                NotebookGitBinding reloadedBinding =
                    notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
                Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
                return new PublishedState(
                    reloadedBinding.getAcceptedGitObjectId(), reloadedNote.getContent());
              });
      assertThat(publishedState.acceptedHead(), equalTo(winner.head().getName()));
      assertThat(publishedState.noteContent(), equalTo(winner.content()));

      Notebook reloadedNotebook =
          inCommittedTransaction(
              transactionManager,
              () -> notebookRepository.findById(notebook.getId()).orElseThrow());
      ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(reloadedNotebook);
      try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
        ObjectId downloadedHead = GitBundleTestReader.fetchHead(readBack, downloaded.getBody());
        assertThat(downloadedHead, equalTo(winner.head()));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(readBack, downloadedHead, "note.md"),
            equalTo(winner.content()));
      }
    } finally {
      releaseBinding.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private Callable<PublicationAttempt> publishAfterBarrier(
      RequestAttributes requestAttributes,
      CyclicBarrier barrier,
      Notebook notebook,
      NotebookGitBinding binding,
      Proposal proposal) {
    return () -> {
      RequestContextHolder.setRequestAttributes(requestAttributes);
      try {
        barrier.await(10, TimeUnit.SECONDS);
        try {
          String publishedHead =
              controller.publishNotebookGitProposal(
                  notebook.getId(), binding.getAcceptedGitObjectId(), proposal.bundleBytes());
          return new PublicationAttempt(publishedHead, null);
        } catch (ResponseStatusException rejection) {
          return new PublicationAttempt(null, rejection);
        }
      } finally {
        RequestContextHolder.resetRequestAttributes();
      }
    };
  }

  private Proposal proposal(NotebookGitBinding binding, String content) throws Exception {
    byte[] bundleBytes =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile("note.md", content)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      return new Proposal(
          bundleBytes, GitBundleTestReader.fetchHead(repository, bundleBytes), content);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for test coordination");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }

  private record Proposal(byte[] bundleBytes, ObjectId head, String content) {}

  private record PublicationAttempt(String acceptedHead, ResponseStatusException rejection) {}

  private record PublishedState(String acceptedHead, String noteContent) {}
}
