package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.DonutApplication;
import com.odde.donut.configs.FlyWayFreeVersionRealMigration;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real ready-event wiring, kept outside the shared test-profile application context. */
class NotebookGitStartupServicesProbeTest extends NotebookGitControllerTestBase {
  @Autowired AcceptedWebChangeService acceptedWebChangeService;

  @Test
  void readyEventFinishesFlywayBeforeAnIndependentNoChangeAcceptedOperation() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("startup probe").content(NOTE).please();
    var binding = snapshotCurrentPortableTree(notebook);
    var historyBefore = acceptedHistory(notebook);
    long objectsBefore = countNativeObjectStoreRows(binding.getId());
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
                    observed.add("Flyway complete");
                  }

                  @Override
                  public String getCallbackName() {
                    return "startup-services-probe";
                  }
                })
            .load();

    // The production listener is disabled in the test profile. This small event context
    // activates that exact listener, while consuming the suite's real service proxy and
    // disposable worktree database; it creates no extra backend application/Hikari pool.
    try (var startup = new AnnotationConfigApplicationContext()) {
      startup.registerBean(Flyway.class, () -> flyway);
      startup.register(FlyWayFreeVersionRealMigration.class);
      startup.registerBean(
          ReadyConsumer.class,
          () ->
              new ReadyConsumer(
                  acceptedWebChangeService,
                  new JdbcTemplate(dataSource),
                  flyway,
                  notebook.getId(),
                  observed));
      startup.refresh();
      startup.publishEvent(
          new ApplicationReadyEvent(
              new SpringApplication(DonutApplication.class),
              new String[0],
              startup,
              Duration.ZERO));
    }

    assertThat(observed, is(List.of("Flyway complete", "consumer", "accepted operation")));
    assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(false));
    assertThat(acceptedHistory(notebook), equalTo(historyBefore));
    assertThat(countNativeObjectStoreRows(binding.getId()), is(objectsBefore));
  }

  static class ReadyConsumer {
    private final AcceptedWebChangeService acceptedChanges;
    private final JdbcTemplate database;
    private final Flyway flyway;
    private final Integer notebookId;
    private final List<String> observed;

    ReadyConsumer(
        AcceptedWebChangeService acceptedChanges,
        JdbcTemplate database,
        Flyway flyway,
        Integer notebookId,
        List<String> observed) {
      this.acceptedChanges = acceptedChanges;
      this.database = database;
      this.flyway = flyway;
      this.notebookId = notebookId;
      this.observed = observed;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public void consume() throws UnexpectedNoAccessRightException {
      assertThat(observed, is(List.of("Flyway complete")));
      assertThat(flyway.info().pending().length, is(0));
      assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(false));
      observed.add("consumer");
      assertThat(
          database.queryForList(
              "SELECT CONCAT(column_name, ':', character_maximum_length, ':', collation_name) "
                  + "FROM information_schema.columns WHERE table_schema = DATABASE() "
                  + "AND table_name = 'memory_tracker' "
                  + "AND column_name IN ('property_key', 'property_value') ORDER BY column_name",
              String.class),
          is(
              List.of(
                  "property_key:255:utf8mb4_0900_ai_ci", "property_value:255:utf8mb4_0900_ai_ci")));
      assertThat(
          database.queryForList(
              "SELECT column_name FROM information_schema.statistics "
                  + "WHERE table_schema = DATABASE() AND table_name = 'memory_tracker' "
                  + "AND index_name = 'uq_memory_tracker_user_note_type_property' "
                  + "AND non_unique = 0 ORDER BY seq_in_index",
              String.class),
          is(List.of("user_id", "note_id", "type", "property_key", "property_value")));
      acceptedChanges.apply(
          notebookId,
          () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(true));
            observed.add("accepted operation");
            return notebookId;
          },
          ignored -> "Startup services probe",
          Timestamp.from(Instant.now()));
      assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(false));
    }
  }
}
