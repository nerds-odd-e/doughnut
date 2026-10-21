package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Grade;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RecallPromptConcurrentAnswerRecoveryControllerTest
    extends RecallPromptAnswerRecoveryTestBase {
  @Autowired EntityManagerFactory entityManagerFactory;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void overlappingPreloadedRequestRecoversCommitOrAcceptsAfterRollback(boolean rollback)
      throws Exception {
    RecallPrompt prompt = createPrompt(false);
    User owner = currentUser.getUser();
    CountDownLatch preloaded = new CountDownLatch(1);
    CountDownLatch written = new CountDownLatch(1);
    CountDownLatch attemptingAnswer = new CountDownLatch(1);
    CountDownLatch releaseWriter = new CountDownLatch(1);
    AtomicInteger originalAnswer = new AtomicInteger();
    AtomicReference<Evidence> originalEvidence = new AtomicReference<>();
    var executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> recovery =
          executor.submit(
              ConcurrentControllerTestSupport.inIsolatedRequest(
                  currentUser,
                  owner,
                  () -> {
                    EntityManager requestEntityManager = entityManagerFactory.createEntityManager();
                    TransactionSynchronizationManager.bindResource(
                        entityManagerFactory, new EntityManagerHolder(requestEntityManager));
                    try {
                      RecallPrompt loaded = repository.findById(prompt.getId()).orElseThrow();
                      assertThat(loaded.getAnswer(), nullValue());
                      preloaded.countDown();
                      ConcurrentControllerTestSupport.await(written);
                      return inCommittedTransaction(
                          transactionManager,
                          () -> {
                            assertThat(entityManager.contains(loaded), is(true));
                            attemptingAnswer.countDown();
                            return accept(loaded, choiceAnswer(1)).getAnswer().getId();
                          });
                    } finally {
                      TransactionSynchronizationManager.unbindResource(entityManagerFactory);
                      requestEntityManager.close();
                    }
                  }));
      ConcurrentControllerTestSupport.await(preloaded);
      Future<?> writer =
          executor.submit(
              ConcurrentControllerTestSupport.inIsolatedRequest(
                  currentUser,
                  owner,
                  () -> {
                    try {
                      inCommittedTransaction(
                          transactionManager,
                          () -> {
                            RecallPrompt loaded =
                                repository.findByIdForUpdate(prompt.getId()).orElseThrow();
                            originalAnswer.set(accept(loaded, timedAnswer(0)).getAnswer().getId());
                            entityManager.flush();
                            entityManager.refresh(loaded.requireMemoryTracker());
                            originalEvidence.set(snapshot(loaded));
                            written.countDown();
                            ConcurrentControllerTestSupport.await(releaseWriter);
                            if (rollback) throw new OriginalRolledBack();
                          });
                    } catch (OriginalRolledBack expected) {
                      // Deliberately release the original acceptance by rolling it back.
                    }
                    return null;
                  }));
      ConcurrentControllerTestSupport.await(attemptingAnswer);
      ConcurrentControllerTestSupport.assertQueued(recovery);
      releaseWriter.countDown();
      writer.get(10, TimeUnit.SECONDS);
      int recoveredId = recovery.get(10, TimeUnit.SECONDS);
      Evidence persisted = evidence(prompt.getId());
      assertThat(persisted.answer().get("id").intValue(), equalTo(recoveredId));
      assertThat(persisted.answer().get("choiceIndex").intValue(), equalTo(rollback ? 1 : 0));
      assertThat(persisted.logs().size(), equalTo(1));
      assertThat(persisted.logs().get(0).get("answerId").intValue(), equalTo(recoveredId));
      assertThat(
          persisted.logs().get(0).get("productOutcome").textValue(),
          equalTo((rollback ? Grade.AGAIN : Grade.GOOD).name()));
      assertThat(persisted.recallCount(), equalTo(1));
      if (!rollback) {
        assertThat(recoveredId, equalTo(originalAnswer.get()));
        assertThat(persisted, equalTo(originalEvidence.get()));
      }
    } finally {
      written.countDown();
      releaseWriter.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private static class OriginalRolledBack extends RuntimeException {}
}
