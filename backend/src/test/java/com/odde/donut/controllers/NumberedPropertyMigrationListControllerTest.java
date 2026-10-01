package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationListControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;

  @Test
  void missingBaseOrdersSparseSuffixesAndPreservesOtherAuthoredKeys() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content(
                "---\ntype: Note\ntopic 10: [C, D]\ntopic 2: A\ntopic 5: B\n"
                    + "url 2: https://example.com\nimage 2: img\ntype 2: Relationship\n"
                    + "topic two: word\n---\nBody")
            .please();
    List<MemoryTracker> trackers =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note stored = noteRepository.findById(note.getId()).orElseThrow();
              return List.of(
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic 2").please(),
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic 5").please(),
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 10")
                      .propertyValue("D")
                      .please());
            });
    snapshotCurrentPortableTree(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    assertContentAndFocuses(
        note,
        "---\ntype: Note\ntopic: [\"A\", \"B\", \"C\", \"D\"]\n"
            + "url: [\"https://example.com\"]\nimage 2: img\ntype 2: Relationship\n"
            + "topic two: word\n---\nBody",
        Map.of(
            trackers.get(0).getId(), new PropertyFocus("topic", "A"),
            trackers.get(1).getId(), new PropertyFocus("topic", "B"),
            trackers.get(2).getId(), new PropertyFocus("topic", "D")));
  }

  @Test
  void existingListKeepsItsOrderAndItemTrackersWhileDeduplicatingValues() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content(
                "---\ntype: Note\ntopic 10: [D, B, E]\ntopic: [B, A, B]\n"
                    + "topic 2: C\n---\nBody")
            .please();
    List<MemoryTracker> trackers =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note stored = noteRepository.findById(note.getId()).orElseThrow();
              return List.of(
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic").propertyValue("B").please(),
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic 2").please(),
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 10")
                      .propertyValue("E")
                      .please());
            });
    snapshotCurrentPortableTree(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    assertContentAndFocuses(
        note,
        "---\ntype: Note\ntopic: [\"B\", \"A\", \"C\", \"D\", \"E\"]\n---\nBody",
        Map.of(
            trackers.get(0).getId(), new PropertyFocus("topic", "B"),
            trackers.get(1).getId(), new PropertyFocus("topic", "C"),
            trackers.get(2).getId(), new PropertyFocus("topic", "E")));
  }

  private void assertContentAndFocuses(
      Note note, String content, Map<Integer, PropertyFocus> focuses) {
    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Note stored = noteRepository.findById(note.getId()).orElseThrow();
            assertThat(noteController.showNote(stored).getNote().getContent(), equalTo(content));
            assertThat(
                noteController.getNoteInfo(stored).getMemoryTrackers().stream()
                    .collect(Collectors.toMap(MemoryTracker::getId, MemoryTracker::propertyFocus)),
                equalTo(focuses));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }
}
