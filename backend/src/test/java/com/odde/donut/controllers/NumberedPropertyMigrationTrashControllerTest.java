package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NumberedPropertyMigration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationTrashControllerTest extends NotebookGitWebContentControllerTestBase {
  private static final String CONSOLIDATED = "---\ntype: Note\ntopic: [\"A\", \"B\"]\n---\nBody";
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;

  @Test
  void undoTrashRestoresTheConsolidatedListWithOriginalLearningAndHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Subject")
            .content("---\ntype: Note\ntopic: A\ntopic 2: B\n---\nBody")
            .please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .propertyKey("topic 2")
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please());
    snapshotCurrentPortableTree(notebook);
    String originalHistory = recallHistory(tracker.getId());
    noteController.trashNote(noteRepository.findById(note.getId()).orElseThrow(), leaveDeadLinks());

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    assertThat(tipText(acceptedHistory(notebook), "_trash/Subject.md"), equalTo(CONSOLIDATED));
    noteController.undoTrashNote(
        noteRepository.findById(note.getId()).orElseThrow(), undoTo("Subject", null));

    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Note restored = noteRepository.findById(note.getId()).orElseThrow();
            var shown = noteController.showNote(restored);
            assertThat(shown.getId(), equalTo(note.getId()));
            assertThat(shown.getNote().isTrashed(), equalTo(false));
            assertThat(shown.getNote().getContent(), equalTo(CONSOLIDATED));
            MemoryTracker retained =
                memoryTrackerController.showMemoryTracker(
                    memoryTrackerRepository.findById(tracker.getId()).orElseThrow());
            assertThat(retained.getId(), equalTo(tracker.getId()));
            assertThat(retained.getNote().getId(), equalTo(note.getId()));
            assertThat(retained.propertyFocus(), equalTo(new PropertyFocus("topic", "B")));
            assertThat(retained.isActive(), equalTo(true));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
    assertThat(recallHistory(tracker.getId()), equalTo(originalHistory));
  }

  private String recallHistory(Integer trackerId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            return objectMapper.writeValueAsString(
                memoryTrackerController.getRecallHistory(
                    memoryTrackerRepository.findById(trackerId).orElseThrow()));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }
}
