package com.odde.donut.controllers;

import static com.odde.donut.controllers.ConcurrentControllerTestSupport.assertQueued;
import static com.odde.donut.controllers.ConcurrentControllerTestSupport.await;
import static com.odde.donut.controllers.ConcurrentControllerTestSupport.inIsolatedRequest;

import com.odde.donut.controllers.currentUser.CurrentUser;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared worker lifecycle, bounded coordination, and per-worker request/authorization setup for
 * concurrent Notebook-Git controller proofs. Each worker receives an independent current-user
 * holder (via {@link ThreadLocalCurrentUser}) even when it represents the same owner, plus a fresh
 * servlet request.
 */
final class NotebookGitConcurrentWriterTestSupport {

  private NotebookGitConcurrentWriterTestSupport() {}

  static <F, S> Result<F, S> runInQueuedOrder(
      PlatformTransactionManager transactionManager,
      NotebookGitBindingRepository bindingRepository,
      CurrentUser currentUser,
      User ownerUser,
      Integer notebookId,
      Callable<F> firstCall,
      Callable<S> secondCall)
      throws Exception {
    CountDownLatch bindingLocked = new CountDownLatch(1);
    CountDownLatch releaseBinding = new CountDownLatch(1);
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(3);

    try {
      Future<?> lockHolder =
          executor.submit(
              () -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transaction.executeWithoutResult(
                    ignored -> {
                      bindingRepository.findByNotebookIdForUpdate(notebookId).orElseThrow();
                      bindingLocked.countDown();
                      await(releaseBinding);
                    });
              });
      await(bindingLocked);

      Future<F> first =
          executor.submit(
              inIsolatedRequest(
                  currentUser,
                  ownerUser,
                  () -> {
                    firstStarted.countDown();
                    return firstCall.call();
                  }));
      await(firstStarted);
      assertQueued(first);

      Future<S> second =
          executor.submit(
              inIsolatedRequest(
                  currentUser,
                  ownerUser,
                  () -> {
                    secondStarted.countDown();
                    return secondCall.call();
                  }));
      await(secondStarted);
      assertQueued(second);

      releaseBinding.countDown();
      lockHolder.get(10, TimeUnit.SECONDS);
      return new Result<>(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    } finally {
      releaseBinding.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  record Result<F, S>(F first, S second) {}
}
