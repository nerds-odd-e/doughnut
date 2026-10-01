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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationAliasRefusalControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void newlyConsolidatedAliasRefusesChangedReaderCardinality(boolean previouslyResolved)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Target")
            .content("---\ntype: Note\naliases 2: CAFÉ\n---\nBody")
            .please();
    var existingBuilder = makeMe.aNote().notebook(notebook).title("Existing");
    if (previouslyResolved) existingBuilder.aliases("Café");
    Note existing = existingBuilder.please();
    Note source =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Source")
            .content("See [[café]] and [[cafe]].")
            .please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
              .propertyKey("aliases 2")
              .afterNthStrictRecall(2)
              .please();
          return null;
        });
    snapshotCurrentPortableTree(notebook);
    var links = noteController.showNote(source).getWikiLinks();
    assertThat(links.size(), equalTo(previouslyResolved ? 1 : 0));
    if (previouslyResolved)
      assertThat(links.getFirst().getDestinationNoteId(), equalTo(existing.getId()));
    var history = acceptedHistory(notebook);
    var before = state(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        containsString("Inconsistent reader resolution: café"));

    assertThat(state(notebook), equalTo(before));
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
