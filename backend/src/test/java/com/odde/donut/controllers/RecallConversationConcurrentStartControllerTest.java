package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RecallConversationConcurrentStartControllerTest
    extends ConversationMessageControllerTestBase {
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;
  @Autowired RecallPromptRepository recallPromptRepository;
  private final String fixtureUser = "recall-help-concurrent-" + UUID.randomUUID();

  @BeforeEach
  void replaceCommittedSetupUser() {
    String setupUser = currentUser.getUser().getExternalIdentifier();
    inCommittedTransaction(
        transactionManager, () -> deleteByUserExternalIdentifierLike(entityManager, setupUser));
    currentUser.setUser(
        inCommittedTransaction(transactionManager, () -> makeMe.aUser(fixtureUser).please()));
  }

  @AfterEach
  void cleanupCommittedFixture() {
    inCommittedTransaction(
        transactionManager,
        () -> {
          entityManager
              .createQuery(
                  "DELETE FROM Conversation c WHERE c.conversationInitiator.externalIdentifier = :user")
              .setParameter("user", fixtureUser)
              .executeUpdate();
          deleteByUserExternalIdentifierLike(entityManager, fixtureUser);
        });
  }

  @Test
  void overlappingRequestsReturnOnePrivateDiscussion() throws Exception {
    RecallPrompt prompt =
        inCommittedTransaction(
            transactionManager,
            () -> {
              currentUser.setUser(entityManager.find(User.class, currentUser.getUser().getId()));
              Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
              return makeMe
                  .aRecallPrompt()
                  .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
                  .withMcqForNote(note)
                  .answerChoiceIndex(1)
                  .please();
            });
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(2);
    ExecutorService executor = Executors.newFixedThreadPool(3);
    try {
      Future<?> lockHolder =
          executor.submit(
              () ->
                  inCommittedTransaction(
                      transactionManager,
                      () -> {
                        recallPromptRepository.findByIdForUpdate(prompt.getId()).orElseThrow();
                        locked.countDown();
                        ConcurrentControllerTestSupport.await(release);
                      }));
      ConcurrentControllerTestSupport.await(locked);
      Future<Conversation> first = startInWorker(executor, prompt, started);
      Future<Conversation> second = startInWorker(executor, prompt, started);
      ConcurrentControllerTestSupport.await(started);
      ConcurrentControllerTestSupport.assertQueued(first);
      ConcurrentControllerTestSupport.assertQueued(second);
      release.countDown();
      lockHolder.get(10, TimeUnit.SECONDS);

      Conversation firstResult = first.get(10, TimeUnit.SECONDS);
      Conversation secondResult = second.get(10, TimeUnit.SECONDS);

      assertThat(secondResult.getId(), equalTo(firstResult.getId()));
      assertThat(conversationRepository.findBySubjectRecallPromptIn(List.of(prompt)), hasSize(1));
    } finally {
      release.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private Future<Conversation> startInWorker(
      ExecutorService executor, RecallPrompt prompt, CountDownLatch started) {
    return executor.submit(
        ConcurrentControllerTestSupport.inIsolatedRequest(
            currentUser,
            currentUser.getUser(),
            () -> {
              RecallPrompt loaded = recallPromptRepository.findById(prompt.getId()).orElseThrow();
              started.countDown();
              return controller.startConversationAboutRecallPrompt(loaded);
            }));
  }
}
