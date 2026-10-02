package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NumberedPropertyMigrationConcurrencyControllerTest
    extends NumberedPropertyMigrationCommittedTestSupport {
  @Autowired NumberedPropertyMigration migration;
  @Autowired ApplicationContext applicationContext;

  @Test
  void overlappingLegacyPreflightsAcceptOneChangeAndKeepDuplicateSurvivorLearningStable()
      throws Exception {
    var book = createGitBackedNotebook("Concurrent");
    var note =
        makeMe
            .aNote()
            .notebook(book)
            .title("Carrier")
            .content("---\ntype: Note\ntopic: [A]\ntopic 2: A\n---\nCarrier")
            .please();
    var trackers = seedDuplicateWithClosure(note);
    snapshotCurrentPortableTree(book);
    var originalHistory = acceptedHistory(book);
    var originalLearning = learning(trackers.getLast());
    deletionClosure(trackers.getFirst());
    var prepared = new CountDownLatch(2);
    var release = new CountDownLatch(1);
    var arrivals = new AtomicInteger();
    var preparation = (Advised) applicationContext.getBean("numberedPropertyMigrationPreparation");
    // Wrap the existing transactional advice: both real preflights commit before either accepts.
    MethodInterceptor synchronizePreflights =
        invocation -> {
          Object result = invocation.proceed();
          if (invocation.getMethod().getName().equals("prepare")
              && invocation.getArguments()[0].equals(book.getId())
              && arrivals.incrementAndGet() <= 2) {
            prepared.countDown();
            NotebookGitConcurrentWriterTestSupport.await(release);
          }
          return result;
        };
    var executor = Executors.newFixedThreadPool(2);
    preparation.addAdvice(0, synchronizePreflights);
    var at = testabilitySettings.getCurrentUTCTimestamp();
    try {
      var first = executor.submit(() -> migration.run(at));
      var second = executor.submit(() -> migration.run(at));
      NotebookGitConcurrentWriterTestSupport.await(prepared);
      assertThat(arrivals.get(), equalTo(2));
      assertThat(first.isDone(), is(false));
      assertThat(second.isDone(), is(false));
      release.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS), equalTo(Map.of()));
      assertThat(second.get(10, TimeUnit.SECONDS), equalTo(Map.of()));
    } finally {
      release.countDown();
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
      preparation.removeAdvice(synchronizePreflights);
    }

    var completed = noteState(note);
    var binding = bindingState(book);
    var history = acceptedHistory(book);
    assertThat(completed.content(), equalTo("---\ntype: Note\ntopic: [\"A\"]\n---\nCarrier"));
    assertThat(completed.trackerIds(), equalTo(List.of(trackers.getLast())));
    assertThat(history.parents(), equalTo(originalHistory.commits()));
    assertThat(learning(trackers.getLast()), equalTo(originalLearning));
    assertDuplicateClosureDeleted(trackers.getFirst());

    assertThat(migration.run(at), equalTo(Map.of()));
    assertThat(noteState(note), equalTo(completed));
    assertThat(bindingState(book), equalTo(binding));
    assertThat(acceptedHistory(book), equalTo(history));
    assertThat(learning(trackers.getLast()), equalTo(originalLearning));
    assertDuplicateClosureDeleted(trackers.getFirst());
  }
}
