package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoteTitleTest {

  @Test
  void fullwidth_slash_is_literal_title_text() {
    NoteTitle noteTitle = new NoteTitle("colour／hue／／tone");
    assertThat(noteTitle.getRecallTitleFragments().getFirst().stem(), equalTo("colour／hue／／tone"));
  }

  @Test
  void with_qualifier() {
    NoteTitle noteTitle = new NoteTitle("cat (animal)");
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("animal")));
  }

  @Test
  void qualifier_does_not_split_on_fullwidth_slash() {
    NoteTitle noteTitle = new NoteTitle("cat (a／b)");
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("a／b")));
  }

  @Test
  void recallTitleFragments_includeMarkedTildeSuffixFragments() {
    NoteTitle noteTitle = new NoteTitle("~logy／~logical");
    assertThat(
        noteTitle.getRecallTitleFragments().stream().map(TitleFragment::stem).toList(),
        containsInAnyOrder("logy", "logical"));
  }

  @Test
  void recallTitleFragments_fullwidthTildeMarkerStaysInPrimary() {
    NoteTitle noteTitle = new NoteTitle("～によると／によれば");
    assertThat(noteTitle.getRecallTitleFragments().getFirst().stem(), equalTo("によると／によれば"));
  }

  @Test
  void matchesForRecall_acceptsExactLiteralTitle() {
    assertThat(new NoteTitle("colour／color").matchesForRecall("colour／color"), is(true));
  }

  @Test
  void matchesForRecall_rejectsSlashSegmentsWithoutTilde() {
    NoteTitle noteTitle = new NoteTitle("colour／color");
    assertThat(noteTitle.matchesForRecall("colour"), is(false));
    assertThat(noteTitle.matchesForRecall("color"), is(false));
  }

  @Test
  void matchesForRecall_acceptsMarkedSuffixFragments() {
    NoteTitle noteTitle = new NoteTitle("word／~logical");
    assertThat(noteTitle.matchesForRecall("word"), is(true));
    assertThat(noteTitle.matchesForRecall("logical"), is(true));
  }
}
