package com.odde.donut.services;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteLevelIndex;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteLevelIndexRepository;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two real transactions on separate threads (and connections) refresh a note's level index
 * concurrently while no {@code note_level_index} row exists yet, proving the create path is
 * race-safe rather than a check-then-insert.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NoteLevelIndexServiceConcurrencyTest {

  private static final String FIXTURE_PREFIX = "note-level-index-concurrency-";

  @Autowired MakeMe makeMe;
  @Autowired NoteLevelIndexService noteLevelIndexService;
  @Autowired NoteLevelIndexRepository noteLevelIndexRepository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @AfterEach
  void cleanupCommittedState() {
    inCommittedTransaction(
        transactionManager,
        () -> deleteByUserExternalIdentifierLike(entityManager, FIXTURE_PREFIX + "%"));
  }

  @Test
  void concurrentFirstRefreshesBothCompleteWithOneCorrectRow() throws Exception {
    Note note =
        inCommittedTransaction(
            transactionManager,
            () -> {
              User user = makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please();
              return makeMe
                  .aNote()
                  .notebookOwnedBy(user)
                  .content("---\nnote_level: 3\n---\n\nbody")
                  .please();
            });

    CyclicBarrier barrier = new CyclicBarrier(2);
    Callable<Void> refreshTheSameNote =
        () -> {
          barrier.await();
          noteLevelIndexService.refreshForNote(note);
          return null;
        };

    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<Future<Void>> futures;
    try {
      futures = executor.invokeAll(List.of(refreshTheSameNote, refreshTheSameNote));
    } finally {
      executor.shutdown();
    }
    for (Future<Void> future : futures) {
      future.get();
    }

    inCommittedTransaction(
        transactionManager,
        () -> {
          List<NoteLevelIndex> rows =
              noteLevelIndexRepository.findById(note.getId()).stream().toList();
          assertThat(rows, hasSize(1));
          assertThat(rows.getFirst().getLevel(), equalTo(3));
          return null;
        });
  }
}
