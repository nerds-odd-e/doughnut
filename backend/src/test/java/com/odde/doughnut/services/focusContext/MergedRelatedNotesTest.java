package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MergedRelatedNotesTest {

  private static FocusContextNote note(String notebook, String title, String content) {
    return new FocusContextNote(notebook, title, "", 1, List.of(), null, content, false);
  }

  @Test
  void addAllDedupsByNotebookAndTitleWithFirstSeenWinsAcrossCalls() {
    MergedRelatedNotes merged = new MergedRelatedNotes();
    FocusContextNote first = note("NB", "Shared", "first content");
    FocusContextNote second = note("NB", "Shared", "second content");
    FocusContextNote other = note("NB", "Other", "other content");

    merged.addAll(List.of(first));
    merged.addAll(List.of(second, other));

    assertThat(merged.asList(), contains(sameInstance(first), sameInstance(other)));
  }

  @Test
  void excludePreventsMatchingNoteFromLaterAddAll() {
    MergedRelatedNotes merged = new MergedRelatedNotes();
    FocusContextNote related = note("NB", "Related", "ok");
    merged.exclude("NB", "Focus");

    merged.addAll(List.of(note("NB", "Focus", "should not appear"), related));

    assertThat(merged.asList(), contains(sameInstance(related)));
  }

  @Test
  void asListReturnsSnapshotIndependentOfFurtherMutation() {
    MergedRelatedNotes merged = new MergedRelatedNotes();
    FocusContextNote first = note("NB", "A", "a");
    FocusContextNote second = note("NB", "B", "b");
    merged.addAll(List.of(first));

    List<FocusContextNote> snapshot = merged.asList();
    merged.addAll(List.of(second));

    assertThat(snapshot, contains(sameInstance(first)));
    assertThat(merged.asList(), contains(sameInstance(first), sameInstance(second)));
  }
}
