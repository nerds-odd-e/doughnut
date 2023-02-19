package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
    assertThat(noteTitle.getTitles(), hasSize(2));
    assertThat(noteTitle.getSubtitles(), hasSize(1));
  }
}
