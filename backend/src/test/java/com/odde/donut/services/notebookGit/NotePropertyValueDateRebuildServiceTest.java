package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotePropertyIndex;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.services.NotePropertyIndexService;
import com.odde.donut.testability.SpringTestBase;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotePropertyValueDateRebuildServiceTest extends SpringTestBase {
  private static final Timestamp A_CREATED = Timestamp.from(Instant.parse("2026-09-01T08:00:00Z"));
  private static final Instant HISTORY_START = Instant.parse("2026-09-02T08:00:00Z");
  private static final Timestamp SAVED = Timestamp.from(Instant.parse("2026-09-08T10:00:00Z"));
  private static final Instant RESET = Instant.parse("2026-09-20T10:00:00Z");
  private static final Timestamp KEPT = Timestamp.from(Instant.parse("2026-09-05T12:00:00Z"));

  @Autowired NotePropertyValueDateRebuildService rebuildService;
  @Autowired NotebookGitHistoryService notebookGitHistoryService;
  @Autowired AcceptedWebChangeService acceptedWebChangeService;
  @Autowired NotePropertyIndexService notePropertyIndexService;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;
  @Autowired EntityManager entityManager;

  private Notebook notebook;
  private Note a;
  private Note b;

  /** Root commit holds A with {@code topic}; a later web save adds A's {@code origin} and B. */
  @BeforeEach
  void historyWithARootAndALaterSave() throws Exception {
    notebook = makeMe.aNotebook().please();
    a =
        makeMe
            .aNote("A")
            .notebook(notebook)
            .content("---\ntopic: physics\n---\nbody")
            .createdAt(A_CREATED)
            .please();
    notePropertyIndexService.refreshForNote(a);
    makeMe.entityPersister.flush();
    notebookGitHistoryService.startHistory(notebook, HISTORY_START);
    acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          a.setContent("---\ntopic: physics\norigin: lab\n---\nbody");
          notePropertyIndexService.refreshForNote(a);
          b =
              makeMe
                  .aNote("B")
                  .notebook(notebook)
                  .content("---\ncolors: [x]\n---\nbody")
                  .createdAt(SAVED)
                  .please();
          notePropertyIndexService.refreshForNote(b);
          return null;
        },
        result -> "Save",
        SAVED);
  }

  @Test
  void datesUndatedValuesFromHistoryAndAtCreationForValuesInTheRoot() {
    clearDates();

    assertThat(rebuildService.rebuildOne(), equalTo(true));

    assertThat(changedAt(a, "topic", ""), equalTo(A_CREATED));
    assertThat(changedAt(a, "origin", ""), equalTo(SAVED));
    assertThat(changedAt(b, "colors", "x"), equalTo(b.getCreatedAt()));
  }

  @Test
  void aFinishedRebuildLeavesNothingForTheNextRun() {
    clearDates();
    rebuildService.rebuildOne();
    List<Timestamp> rebuilt = allDates();

    assertThat(rebuildService.rebuildOne(), equalTo(false));

    assertThat(allDates(), equalTo(rebuilt));
  }

  @Test
  void aValueOutsideTheRetainedHistoryIsDatedAtItsNotesCreation() {
    Timestamp injected = Timestamp.from(Instant.parse("2026-09-10T09:00:00Z"));
    Note uncommitted =
        makeMe
            .aNote("C")
            .notebook(notebook)
            .content("---\ntopic: chemistry\n---\nbody")
            .createdAt(injected)
            .please();
    notePropertyIndexService.refreshForNote(uncommitted);
    clearDates();

    rebuildService.rebuildOne();

    assertThat(changedAt(uncommitted, "topic", ""), equalTo(injected));
    assertThat(rebuildService.rebuildOne(), equalTo(false));
  }

  @Test
  void aDatedValueIsNotRewritten() {
    clearDates();
    setDate(a, "topic", KEPT);

    rebuildService.rebuildOne();

    assertThat(changedAt(a, "topic", ""), equalTo(KEPT));
    assertThat(changedAt(a, "origin", ""), equalTo(SAVED));
  }

  @Test
  void aResetKeepsDatedValues() {
    makeMe.entityPersister.flushAndClear();

    notebookGitHistoryService.resetHistory(reloaded(notebook), RESET);
    makeMe.entityPersister.flushAndClear();

    assertThat(changedAt(a, "origin", ""), equalTo(SAVED));
    assertThat(changedAt(b, "colors", "x"), equalTo(SAVED));
  }

  @Test
  void afterAResetEveryUndatedValueIsDatedAtItsNotesCreation() {
    makeMe.entityPersister.flushAndClear();
    notebookGitHistoryService.resetHistory(reloaded(notebook), RESET);
    clearDates();

    assertThat(rebuildService.rebuildOne(), equalTo(true));

    assertThat(changedAt(a, "topic", ""), equalTo(A_CREATED));
    assertThat(changedAt(a, "origin", ""), equalTo(A_CREATED));
    assertThat(changedAt(b, "colors", "x"), equalTo(b.getCreatedAt()));
  }

  /** As a notebook from before value dating: none of its values has a date. */
  private void clearDates() {
    makeMe.entityPersister.flush();
    entityManager
        .createQuery(
            "UPDATE NotePropertyIndex i SET i.changedAt = NULL WHERE i.note.notebook.id = :id")
        .setParameter("id", notebook.getId())
        .executeUpdate();
    makeMe.entityPersister.flushAndClear();
  }

  private void setDate(Note note, String key, Timestamp at) {
    rows(note, key, "").forEach(row -> row.setChangedAt(at));
    makeMe.entityPersister.flushAndClear();
  }

  private Timestamp changedAt(Note note, String key, String value) {
    return rows(note, key, value).getFirst().getChangedAt();
  }

  private List<NotePropertyIndex> rows(Note note, String key, String value) {
    return notePropertyIndexRepository.findByNote_IdAndPropertyKeyAndPropertyValue(
        note.getId(), key, value);
  }

  private List<Timestamp> allDates() {
    return List.of(
        changedAt(a, "topic", ""), changedAt(a, "origin", ""), changedAt(b, "colors", "x"));
  }

  private Notebook reloaded(Notebook target) {
    return makeMe.entityPersister.find(Notebook.class, target.getId());
  }
}
