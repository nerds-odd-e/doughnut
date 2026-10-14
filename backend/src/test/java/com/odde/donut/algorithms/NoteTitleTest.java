package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class NoteTitleTest {

  @Test
  void fullwidth_slash_is_literal_title_text() {
    NoteTitle noteTitle = new NoteTitle("colour／hue／／tone");
    assertThat(noteTitle.getClozeTitleFragments().getFirst().stem(), equalTo("colour／hue／／tone"));
  }

  @Test
  void clozeTitleFragments_treatsTrailingBracketAsLiteralTitleText() {
    NoteTitle noteTitle = new NoteTitle("cat(animal)");
    assertThat(
        noteTitle.getClozeTitleFragments().stream().map(TitleFragment::stem).toList(),
        contains("cat(animal)"));
  }

  @Test
  void clozeTitleFragments_treatsLaterTildeAsLiteralTitleText() {
    NoteTitle noteTitle = new NoteTitle("word／~logical");
    assertThat(
        noteTitle.getClozeTitleFragments().stream().map(TitleFragment::stem).toList(),
        contains("word／~logical"));
  }

  @Test
  void clozeTitleFragments_fullwidthTildeMarkerStaysInPrimary() {
    NoteTitle noteTitle = new NoteTitle("～によると／によれば");
    assertThat(noteTitle.getClozeTitleFragments().getFirst().stem(), equalTo("によると／によれば"));
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
  void matchesForRecall_treatsLaterSlashAndTildeAsLiteralTitleText() {
    NoteTitle noteTitle = new NoteTitle("word／~logical");
    assertThat(noteTitle.matchesForRecall("word／~logical"), is(true));
    assertThat(noteTitle.matchesForRecall("word"), is(false));
    assertThat(noteTitle.matchesForRecall("logical"), is(false));
  }

  @Test
  void matchesForRecall_retainsLeadingTildeSuffixBehavior() {
    NoteTitle noteTitle = new NoteTitle("~logical");
    assertThat(noteTitle.matchesForRecall("logical"), is(true));
  }

  @Test
  void matchesForRecall_acceptsTitleWithoutTrailingParenthesizedContent() {
    NoteTitle noteTitle = new NoteTitle("cat(animal)");
    assertThat(noteTitle.matchesForRecall("cat(animal)"), is(true));
    assertThat(noteTitle.matchesForRecall("cat"), is(true));
    assertThat(noteTitle.matchesForRecall("animal"), is(false));
  }
}
