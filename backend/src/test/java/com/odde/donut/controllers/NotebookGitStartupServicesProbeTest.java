package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.DonutApplication;
import com.odde.donut.configs.FlyWayFreeVersionRealMigration;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
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

/** Exact production Flyway ready listener with the shared isolated database and services. */
class NotebookGitStartupServicesProbeTest extends NotebookGitWebContentControllerTestBase {
  @Autowired DataSource dataSource;

  @Test
  void readyEventsRunFlywayWithoutChangingConsolidatedContentAcceptedHistoryOrLearning()
      throws Exception {
    var notebook = createGitBackedNotebook("Startup");
    String content = "---\ntype: Note\ntopic: [A, B]\n---\nCarrier";
    Note note = makeMe.aNote().notebook(notebook).title("Carrier").content(content).please();
    var trackerIds = learnedListTrackers(note);
    var originalLearning = trackerIds.stream().map(this::learning).toList();
    snapshotCurrentPortableTree(notebook);
    var originalHistory = acceptedHistory(notebook);
    String originalHead = binding(notebook).getAcceptedGitObjectId();
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
    try (var startup = new AnnotationConfigApplicationContext()) {
      startup.registerBean(Flyway.class, () -> flyway);
      startup.register(FlyWayFreeVersionRealMigration.class);
      startup.registerBean(
          ReadyObservation.class,
          () -> new ReadyObservation(new JdbcTemplate(dataSource), flyway, observed));
      startup.refresh();
      for (int event = 0; event < 2; event++) {
        startup.publishEvent(
            new ApplicationReadyEvent(
                new SpringApplication(DonutApplication.class),
                new String[0],
                startup,
                Duration.ZERO));
      }
    }
    assertThat(
        observed,
        equalTo(List.of("Flyway complete", "ready consumer", "Flyway complete", "ready consumer")));
    Note stored = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(noteController.showNote(stored).getNote().getContent(), equalTo(content));
    assertThat(
        noteController.getNoteInfo(stored).getMemoryTrackers().stream()
            .map(MemoryTracker::getId)
            .sorted()
            .toList(),
        equalTo(trackerIds.stream().sorted().toList()));
    assertThat(trackerIds.stream().map(this::learning).toList(), equalTo(originalLearning));
    assertThat(binding(notebook).getAcceptedGitObjectId(), equalTo(originalHead));
    assertThat(acceptedHistory(notebook), equalTo(originalHistory));
  }

  static class ReadyObservation {
    private final JdbcTemplate database;
    private final Flyway flyway;
    private final List<String> observed;

    ReadyObservation(JdbcTemplate database, Flyway flyway, List<String> observed) {
      this.database = database;
      this.flyway = flyway;
      this.observed = observed;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    public void consume() {
      assertThat(flyway.info().pending().length, is(0));
      assertThat(TransactionSynchronizationManager.isActualTransactionActive(), is(false));
      observed.add("ready consumer");
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
    }
  }
}
