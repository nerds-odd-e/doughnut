package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.NumberedPropertyMigration;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationControllerTest extends NotebookGitWebContentControllerTestBase {
  private static final String ORIGINAL =
      "---\ntype: Note\nexample of: '[[Run]]'\nexample of 2: '[[Walk]]'\n---\nbody";
  private static final String CONSOLIDATED =
      "---\ntype: Note\nexample of: [\"[[Run]]\", \"[[Walk]]\"]\n---\nbody";
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotePropertyIndexRepository propertyIndexRepository;

  @Test
  void learnedScalarsBecomeOneAcceptedListKeepingBothLearningIdentities() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note run = makeMe.aNote().notebook(notebook).title("Run").please();
    Note walk = makeMe.aNote().notebook(notebook).title("Walk").please();
    Note note = makeMe.aNote().notebook(notebook).title("Carrier").content(ORIGINAL).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            List.of(
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .propertyKey("example of")
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please(),
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .propertyKey("example of 2")
                    .afterNthStrictRecall(3)
                    .recallCount(3)
                    .please()));
    snapshotCurrentPortableTree(notebook);
    var before = acceptedHistory(notebook);
    List<TrackerState> originalTrackers = committedState(notebook).trackers();

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
            var shown = noteController.showNote(reloaded);
            assertThat(shown.getId(), equalTo(note.getId()));
            assertThat(shown.getNote().getContent(), equalTo(CONSOLIDATED));
            assertThat(
                noteController.getNoteInfo(reloaded).getMemoryTrackers().stream()
                    .map(MemoryTracker::getId)
                    .sorted()
                    .toList(),
                equalTo(originalTrackers.stream().map(TrackerState::id).sorted().toList()));
            assertThat(
                shown.getWikiLinks().stream()
                    .map(link -> link.getDestinationNoteId())
                    .sorted()
                    .toList(),
                equalTo(List.of(run.getId(), walk.getId()).stream().sorted().toList()));
            for (int i = 0; i < originalTrackers.size(); i++) {
              TrackerState original = originalTrackers.get(i);
              MemoryTracker retained =
                  memoryTrackerRepository.findById(original.id()).orElseThrow();
              assertThat(
                  retained.propertyFocus(),
                  equalTo(new PropertyFocus("example of", i == 0 ? "[[Run]]" : "[[Walk]]")));
              assertThat(retained.getNextRecallAt(), equalTo(original.nextRecallAt()));
              assertThat(retained.getStability(), equalTo(original.stability()));
              assertThat(retained.getDifficulty(), equalTo(original.difficulty()));
              assertThat(retained.getLastRecalledAt(), equalTo(original.lastRecalledAt()));
              assertThat(retained.getAssimilatedAt(), equalTo(original.assimilatedAt()));
              assertThat(retained.getRemovedFromTracking(), equalTo(original.removed()));
              assertThat(retained.getType(), equalTo(original.type()));
              assertThat(
                  objectMapper.writeValueAsString(
                      memoryTrackerController.getRecallHistory(retained)),
                  equalTo(original.history()));
            }
            assertThat(
                propertyIndexRepository.findByNote_IdOrderByIdAsc(note.getId()).stream()
                    .map(
                        index ->
                            new PropertyFocus(index.getPropertyKey(), index.getPropertyValue()))
                    .toList(),
                contains(
                    new PropertyFocus("example of", "[[Run]]"),
                    new PropertyFocus("example of", "[[Walk]]")));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
    var after = acceptedHistory(notebook);
    assertThat(after.commits(), hasSize(before.commits().size() + 1));
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(tipText(after, "Carrier.md"), equalTo(CONSOLIDATED));
    assertThat(after.tipPaths(), contains("Carrier.md", "Run.md", "Walk.md"));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walkReader = new RevWalk(repository)) {
      var tip =
          walkReader.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)));
      assertThat(
          tip.getAuthorIdent().getName(), equalTo(NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME));
      assertThat(tip.getFullMessage(), equalTo("Consolidate numbered properties"));
    }
  }

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
