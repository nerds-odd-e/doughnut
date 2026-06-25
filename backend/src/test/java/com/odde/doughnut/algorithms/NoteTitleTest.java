package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoteTitleTest {

  @Test
  void escaped_fullwidth_slash_is_literal_inside_segment() {
    NoteTitle noteTitle = new NoteTitle("colour／hue／／tone");
    assertThat(noteTitle.getAliasSegmentsInOrder(), hasSize(2));
    assertThat(noteTitle.getAliasSegmentsInOrder().get(0).stem(), equalTo("colour"));
    assertThat(noteTitle.getAliasSegmentsInOrder().get(1).stem(), equalTo("hue／tone"));
  }

  @Test
  void escaped_fullwidth_slash_keeps_single_segment_title_intact() {
    NoteTitle noteTitle = new NoteTitle("cat／／kitten");
    assertThat(noteTitle.getAliasSegmentsInOrder(), hasSize(1));
    assertThat(noteTitle.getAliasSegmentsInOrder().get(0).stem(), equalTo("cat／kitten"));
  }

  @Test
  void with_qualifier() {
    NoteTitle noteTitle = new NoteTitle("cat (animal)");
    assertThat(noteTitle.getAliasSegmentsInOrder(), hasSize(1));
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("animal")));
  }

  @Test
  void qualifier_does_not_split_on_fullwidth_slash() {
    NoteTitle noteTitle = new NoteTitle("cat (a／b)");
    assertThat(noteTitle.getAliasSegmentsInOrder(), hasSize(1));
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("a／b")));
  }

  @Test
  void recallTitleFragments_includePrimaryAndSuffixFragmentsOnly() {
    NoteTitle noteTitle = new NoteTitle("colour／color");
    assertThat(noteTitle.getRecallTitleFragments(), hasSize(1));
    assertThat(noteTitle.getRecallTitleFragments().get(0).stem(), equalTo("colour"));

    noteTitle = new NoteTitle("~logy／~logical");
    assertThat(noteTitle.getRecallTitleFragments(), hasSize(2));
    assertThat(
        noteTitle.getRecallTitleFragments().stream().map(TitleFragment::stem).toList(),
        containsInAnyOrder("logy", "logical"));

    noteTitle = new NoteTitle("～によると／によれば");
    assertThat(noteTitle.getRecallTitleFragments(), hasSize(1));
    assertThat(noteTitle.getRecallTitleFragments().get(0).stem(), equalTo("によると"));
  }

  @Test
  void matchesForRecall_acceptsPrimaryAndSuffixFragments_notPlainAliases() {
    NoteTitle noteTitle = new NoteTitle("colour／color");
    assertThat(noteTitle.matchesForRecall("colour"), is(true));
    assertThat(noteTitle.matchesForRecall("color"), is(false));
  }
}
