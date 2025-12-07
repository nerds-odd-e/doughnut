package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class NoteTitleTest {

  @Test
  void with_multiple_options() {
    NoteTitle noteTitle = new NoteTitle("cat / kitten");
    assertThat(noteTitle.getTitles(), hasSize(2));
    assertThat(noteTitle.getSubtitles(), hasSize(0));
  }

  @Test
  void with_subtitle() {
    NoteTitle noteTitle = new NoteTitle("cat (animal)");
    assertThat(noteTitle.getTitles(), hasSize(1));
    assertThat(noteTitle.getSubtitles(), hasSize(1));
  }

  @Test
  void replacing() {
    NoteTitle noteTitle = new NoteTitle("~logy / ~logical");
    TitleFragment titleFragment = noteTitle.getTitles().get(0);
    assertThat(titleFragment.replaceLiteralWords("technological", ".."), equalTo("techno.."));
  }

  @Test
  void with_non_breaking_space_after_slash() {
    // Test case for title with non-breaking space (U+00A0) after "/"
    // This simulates the database value "nebulas / nebula" with non-breaking space
    String titleWithNonBreakingSpace = "nebulas /\u00A0nebula";
    NoteTitle noteTitle = new NoteTitle(titleWithNonBreakingSpace);
    assertThat(noteTitle.getTitles(), hasSize(2));
    // Fragments are sorted by length (longest first), so "nebulas" comes before "nebula"
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("nebulas"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("nebula"));
    // Verify matching works with regular space
    assertThat(noteTitle.getTitles().get(0).matches("nebulas"), is(true));
    assertThat(noteTitle.getTitles().get(1).matches("nebula"), is(true));
  }

  @Test
  void with_cjk_ideographic_space() {
    // Test case for title with CJK ideographic space (U+3000) after "/"
    String titleWithCjkSpace = "cat\u3000/\u3000kitten";
    NoteTitle noteTitle = new NoteTitle(titleWithCjkSpace);
    assertThat(noteTitle.getTitles(), hasSize(2));
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("kitten"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("cat"));
    // Verify matching works
    assertThat(noteTitle.getTitles().get(0).matches("kitten"), is(true));
    assertThat(noteTitle.getTitles().get(1).matches("cat"), is(true));
  }

  @Test
  void with_various_unicode_whitespace() {
    // Test case for various Unicode whitespace characters
    // U+2000: En quad, U+2001: Em quad, U+2002: En space, U+2003: Em space
    String titleWithVariousSpaces = "word1\u2000/\u2001word2\u2002/\u2003word3";
    NoteTitle noteTitle = new NoteTitle(titleWithVariousSpaces);
    assertThat(noteTitle.getTitles(), hasSize(3));
    // All whitespace should be normalized, fragments should be clean
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("word3"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("word2"));
    assertThat(noteTitle.getTitles().get(2).stem(), equalTo("word1"));
    // Verify matching works
    assertThat(noteTitle.getTitles().get(0).matches("word3"), is(true));
    assertThat(noteTitle.getTitles().get(1).matches("word2"), is(true));
    assertThat(noteTitle.getTitles().get(2).matches("word1"), is(true));
  }
}
