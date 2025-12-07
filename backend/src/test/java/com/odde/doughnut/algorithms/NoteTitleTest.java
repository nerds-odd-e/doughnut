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
  void normalizes_unicode_whitespace_to_regular_spaces() {
    // Test that various Unicode whitespace characters are normalized to regular spaces
    // U+00A0: non-breaking space, U+3000: CJK ideographic space,
    // U+2000-U+2003: en/em spaces
    NoteTitle noteTitle = new NoteTitle("nebulas /\u00A0nebula");
    assertThat(noteTitle.getTitles(), hasSize(2));
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("nebulas"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("nebula"));

    noteTitle = new NoteTitle("cat\u3000/\u3000kitten");
    assertThat(noteTitle.getTitles(), hasSize(2));
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("kitten"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("cat"));

    noteTitle = new NoteTitle("word1\u2000/\u2001word2\u2002/\u2003word3");
    assertThat(noteTitle.getTitles(), hasSize(3));
    assertThat(noteTitle.getTitles().get(0).stem(), equalTo("word3"));
    assertThat(noteTitle.getTitles().get(1).stem(), equalTo("word2"));
    assertThat(noteTitle.getTitles().get(2).stem(), equalTo("word1"));
  }
}
