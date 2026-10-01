package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NumberedPropertyMigration;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationControllerTest extends NotebookGitWebContentControllerTestBase {
  private static final String ORIGINAL =
      "---\ntype: Note\nexample of: '[[Run]]'\nexample of 2: '[[Walk]]'\n---\nbody";
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;

  @ParameterizedTest
  @ValueSource(strings = {"orphan", "empty-list", "empty-value", "oversize"})
  void anUnmappableFocusLeavesTheWholeNotebookUnchanged(String scenario) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note earlier = makeMe.aNote().notebook(notebook).title("Earlier").content(ORIGINAL).please();
    String content =
        ORIGINAL.replace(
            "example of 2: '[[Walk]]'",
            switch (scenario) {
              case "empty-list" -> "example of 2: []";
              case "empty-value" -> "example of 2: ''";
              case "oversize" -> "example of 2: '" + "x".repeat(256) + "'";
              default -> "example of 2: '[[Walk]]'";
            });
    Note note = makeMe.aNote().notebook(notebook).title("Carrier").content(content).please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aMemoryTrackerFor(noteRepository.findById(earlier.getId()).orElseThrow())
              .propertyKey("example of")
              .afterNthStrictRecall(2)
              .recallCount(2)
              .please();
          makeMe
              .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
              .propertyKey("example of 2")
              .propertyValue(scenario.equals("orphan") ? "orphan" : "")
              .afterNthStrictRecall(3)
              .recallCount(3)
              .please();
        });
    snapshotCurrentPortableTree(notebook);
    var before = acceptedHistory(notebook);
    var beforeState = committedState(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        containsString(
            scenario.equals("oversize") ? "persisted column limit" : "Unmapped tracker focus"));

    assertThat(committedState(notebook), equalTo(beforeState));
    assertThat(acceptedHistory(notebook), equalTo(before));
  }

  private NotebookState committedState(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
            List<String> contents = new ArrayList<>();
            for (Note note : notes) {
              contents.add(noteController.showNote(note).getNote().getContent());
            }
            List<TrackerState> trackers = new ArrayList<>();
            for (MemoryTracker tracker :
                memoryTrackerRepository.findByNote_IdIn(notes.stream().map(Note::getId).toList())) {
              trackers.add(
                  new TrackerState(
                      tracker.getId(),
                      tracker.propertyFocus(),
                      tracker.getType(),
                      tracker.getRemovedFromTracking(),
                      tracker.getAssimilatedAt(),
                      tracker.getNextRecallAt(),
                      tracker.getLastRecalledAt(),
                      tracker.getStability(),
                      tracker.getDifficulty(),
                      objectMapper.writeValueAsString(
                          memoryTrackerController.getRecallHistory(tracker))));
            }
            trackers.sort(Comparator.comparing(TrackerState::id));
            return new NotebookState(contents, trackers);
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  private record NotebookState(List<String> contents, List<TrackerState> trackers) {}

  private record TrackerState(
      Integer id,
      PropertyFocus focus,
      MemoryTrackerType type,
      Boolean removed,
      Timestamp assimilatedAt,
      Timestamp nextRecallAt,
      Timestamp lastRecalledAt,
      Float stability,
      Float difficulty,
      String history) {}
}
