package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.odde.donut.configs.FlyWayFreeVersionRealMigration;
import com.odde.donut.configs.NumberedPropertyStartupMigration;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NumberedPropertyMigration;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Exact production ready-event listeners with the shared services and isolated database. */
class NotebookGitStartupServicesProbeTest extends NumberedPropertyStartupTestSupport {
  @Autowired NumberedPropertyMigration migration;

  @Test
  void readyEventMigratesAfterFlywayReportsRefusalsAndRepeatedEventPreservesAcceptedLearning()
      throws Exception {
    var notebook = createGitBackedNotebook("Startup");
    Note run = makeMe.aNote().notebook(notebook).title("Run").please();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Carrier")
            .content("---\ntype: Note\ntopic 2: '[[Run]]'\n---\nCarrier")
            .please();
    Integer tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .propertyKey("topic 2")
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please()
                    .getId());
    var refusedBook = createGitBackedNotebook("Refused");
    Note refused =
        makeMe
            .aNote()
            .notebook(refusedBook)
            .title("Unmappable")
            .content("---\ntype: Note\ntopic 2: B\n---\nUnmappable")
            .please();
    Integer orphan =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(refused.getId()).orElseThrow())
                    .propertyKey("topic 2")
                    .propertyValue("orphan")
                    .please()
                    .getId());
    snapshotCurrentPortableTree(notebook);
    snapshotCurrentPortableTree(refusedBook);
    var historyBefore = acceptedHistory(notebook);
    var originalLearning = learning(tracker);
    var refusedBefore = noteState(refused);
    var refusedLearning = learning(orphan);
    var refusedBinding = bindingState(refusedBook);
    var refusedHistory = acceptedHistory(refusedBook);
    List<String> observed = new ArrayList<>();
    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .callbacks(
                new Callback() {
                  @Override
                  public boolean supports(Event event, Context context) {
                    return event == Event.AFTER_MIGRATE;
                  }

                  @Override
                  public boolean canHandleInTransaction(Event event, Context context) {
                    return false;
                  }

                  @Override
                  public void handle(Event event, Context context) {
                    assertThat(
                        TransactionSynchronizationManager.isActualTransactionActive(), is(false));
                    // The first Flyway callback still sees legacy content, before the actual
                    // startup caller.
                    if (observed.isEmpty())
                      assertThat(noteState(note).content(), containsString("topic 2:"));
                    observed.add("Flyway complete");
                  }

                  @Override
                  public String getCallbackName() {
                    return "startup-services-probe";
                  }
                })
            .load();
    Logger logger = (Logger) LoggerFactory.getLogger(NumberedPropertyStartupMigration.class);
    Level previousLevel = logger.getLevel();
    var log = new ListAppender<ILoggingEvent>();
    log.start();
    logger.addAppender(log);
    logger.setLevel(Level.WARN);
    // No second Boot context or connection pool: activate the exact production listeners
    // in a small event context using the suite's actual service proxy and database.
    try (var startup = new AnnotationConfigApplicationContext()) {
      startup.registerBean(Flyway.class, () -> flyway);
      startup.registerBean(NumberedPropertyMigration.class, () -> migration);
      startup.register(
          FlyWayFreeVersionRealMigration.class, NumberedPropertyStartupMigration.class);
      startup.registerBean(
          ReadyObservation.class,
          () ->
              new ReadyObservation(
                  new JdbcTemplate(dataSource),
                  flyway,
                  observed,
                  () ->
                      assertThat(
                          noteState(note).content(),
                          equalTo("---\ntype: Note\ntopic: [\"[[Run]]\"]\n---\nCarrier"))));
      startup.refresh();
      publishReady(startup);
      assertThat(observed, is(List.of("Flyway complete", "migrated consumer")));
      var completed = noteState(note);
      var completedLearning = learning(tracker);
      assertThat(completed.trackerIds(), equalTo(List.of(tracker)));
      assertThat(completed.properties(), contains(new PropertyFocus("topic", "[[Run]]")));
      assertThat(completed.references(), contains("Run"));
      assertThat(completed.links(), containsString("\"destinationNoteId\":" + run.getId()));
      assertThat(completedLearning.focus(), equalTo(new PropertyFocus("topic", "[[Run]]")));
      assertThat(completedLearning.history(), equalTo(originalLearning.history()));
      assertThat(completedLearning.next(), equalTo(originalLearning.next()));
      assertThat(completedLearning.last(), equalTo(originalLearning.last()));
      assertThat(completedLearning.assimilated(), equalTo(originalLearning.assimilated()));
      assertThat(completedLearning.stability(), equalTo(originalLearning.stability()));
      assertThat(completedLearning.difficulty(), equalTo(originalLearning.difficulty()));
      var historyAfter = acceptedHistory(notebook);
      var bindingAfter = bindingState(notebook);
      assertThat(historyAfter.parents(), equalTo(historyBefore.commits()));
      assertThat(historyAfter.tipPaths(), contains("Carrier.md", "Run.md"));
      assertThat(tipText(historyAfter, "Carrier.md"), equalTo(completed.content()));
      try (var repository = new InMemoryRepository(new DfsRepositoryDescription());
          var walker = new RevWalk(repository)) {
        var tip =
            walker.parseCommit(
                GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)));
        assertThat(
            tip.getAuthorIdent().getName(), equalTo(NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME));
        assertThat(tip.getFullMessage(), equalTo("Consolidate numbered properties"));
      }
      publishReady(startup);
      assertThat(
          observed,
          is(
              List.of(
                  "Flyway complete", "migrated consumer", "Flyway complete", "migrated consumer")));
      assertThat(noteState(note), equalTo(completed));
      assertThat(learning(tracker), equalTo(completedLearning));
      assertThat(bindingState(notebook), equalTo(bindingAfter));
      assertThat(acceptedHistory(notebook), equalTo(historyAfter));
      assertThat(noteState(refused), equalTo(refusedBefore));
      assertThat(learning(orphan), equalTo(refusedLearning));
      assertThat(bindingState(refusedBook), equalTo(refusedBinding));
      assertThat(acceptedHistory(refusedBook), equalTo(refusedHistory));
      assertThat(
          log.list.stream().map(ILoggingEvent::getFormattedMessage).toList(),
          contains(
              "Numbered-property migration left notebook "
                  + refusedBook.getId()
                  + " unchanged: Note "
                  + refused.getId()
                  + ": Unmapped tracker focus: "
                  + orphan,
              "Numbered-property migration left notebook "
                  + refusedBook.getId()
                  + " unchanged: Note "
                  + refused.getId()
                  + ": Unmapped tracker focus: "
                  + orphan));
    } finally {
      logger.detachAppender(log);
      log.stop();
      logger.setLevel(previousLevel);
    }
  }
}
