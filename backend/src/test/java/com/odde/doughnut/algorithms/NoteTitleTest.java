package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
}
