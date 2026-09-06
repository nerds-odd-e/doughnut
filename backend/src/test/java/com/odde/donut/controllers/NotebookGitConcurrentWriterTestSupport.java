package com.odde.donut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

final class NotebookGitConcurrentWriterTestSupport {

  private NotebookGitConcurrentWriterTestSupport() {}

  static <F, S> Result<F, S> runInQueuedOrder(
      PlatformTransactionManager transactionManager,
      NotebookGitBindingRepository bindingRepository,
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

      Future<F> first = executor.submit(inFreshRequest(firstStarted, firstCall));
      await(firstStarted);
      assertQueued(first);

      Future<S> second = executor.submit(inFreshRequest(secondStarted, secondCall));
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

  private static <T> Callable<T> inFreshRequest(CountDownLatch started, Callable<T> call) {
    return () -> {
      RequestContextHolder.setRequestAttributes(
          new ServletRequestAttributes(new MockHttpServletRequest()));
      try {
        started.countDown();
        return call.call();
      } finally {
        RequestContextHolder.resetRequestAttributes();
      }
    };
  }

  private static void assertQueued(Future<?> writer) {
    assertThrows(TimeoutException.class, () -> writer.get(250, TimeUnit.MILLISECONDS));
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

  record Result<F, S>(F first, S second) {}
}
