package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationSelectorRefusalControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;

  @Test
  void orphanFocusContainingARewrittenSelectorRefusesTheCompleteOperation() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Target")
            .content("---\ntype: Note\ntopic 2: A\n---\nBody")
            .please();
    String item = "Read [[Target#prop:topic%202|detail]]";
    Note valid =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Valid")
            .content("---\ntype: Note\nrelated: ['" + item + "', unchanged]\n---\nBody")
            .please();
    Note orphan =
        makeMe.aNote().notebook(notebook).title("Orphan").content(valid.getContent()).please();
    Integer orphanTracker =
        inCommittedTransaction(
            transactionManager,
            () -> {
              makeMe
                  .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
                  .propertyKey("topic 2")
                  .afterNthStrictRecall(2)
                  .please();
              for (String value : List.of(item, "unchanged"))
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(valid.getId()).orElseThrow())
                    .propertyKey("related")
                    .propertyValue(value)
                    .afterNthStrictRecall(2)
                    .please();
              return makeMe
                  .aMemoryTrackerFor(noteRepository.findById(orphan.getId()).orElseThrow())
                  .propertyKey("related")
                  .propertyValue("extra " + item)
                  .afterNthStrictRecall(3)
                  .please()
                  .getId();
            });
    snapshotCurrentPortableTree(notebook);
    var history = acceptedHistory(notebook);
    var state = state(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        containsString("Unmapped tracker focus: " + orphanTracker));

    assertThat(state(notebook), equalTo(state));
    assertThat(acceptedHistory(notebook), equalTo(history));
  }

  private List<String> state(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            List<String> state = new ArrayList<>();
            var notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
            for (Note note : notes) state.add(noteController.showNote(note).getNote().getContent());
            for (var tracker :
                memoryTrackerRepository.findByNote_IdIn(notes.stream().map(Note::getId).toList())) {
              state.add(
                  objectMapper.writeValueAsString(
                      memoryTrackerController.showMemoryTracker(tracker)));
              state.add(
                  objectMapper.writeValueAsString(
                      memoryTrackerController.getRecallHistory(tracker)));
            }
            return state;
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }
}
