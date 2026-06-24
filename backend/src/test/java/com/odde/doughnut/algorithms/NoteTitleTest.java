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
  void with_aliases() {
    NoteTitle noteTitle = new NoteTitle("cat／kitten");
    assertThat(noteTitle.getTitleAliases(), hasSize(2));
    assertThat(noteTitle.getQualifier(), is(Optional.empty()));
  }

  @Test
  void ascii_slash_does_not_separate_aliases() {
    NoteTitle noteTitle = new NoteTitle("cat / kitten");
    assertThat(noteTitle.getTitleAliases(), hasSize(1));
    assertThat(noteTitle.getTitleAliases().get(0).stem(), equalTo("cat / kitten"));
  }

  @Test
  void with_qualifier() {
    NoteTitle noteTitle = new NoteTitle("cat (animal)");
    assertThat(noteTitle.getTitleAliases(), hasSize(1));
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("animal")));
  }

  @Test
  void qualifier_does_not_split_on_fullwidth_slash() {
    NoteTitle noteTitle = new NoteTitle("cat (a／b)");
    assertThat(noteTitle.getTitleAliases(), hasSize(1));
    assertThat(noteTitle.getQualifier().map(TitleFragment::stem), is(Optional.of("a／b")));
  }

  @Test
  void replacing() {
    NoteTitle noteTitle = new NoteTitle("~logy／~logical");
    TitleFragment titleFragment = noteTitle.getTitleAliases().get(0);
    assertThat(titleFragment.replaceLiteralWords("technological", ".."), equalTo("techno.."));
  }

  @Test
  void normalizes_unicode_whitespace_to_regular_spaces() {
    // Test that various Unicode whitespace characters are normalized to regular spaces
    // U+00A0: non-breaking space, U+3000: CJK ideographic space,
    // U+2000-U+2003: en/em spaces
    NoteTitle noteTitle = new NoteTitle("nebulas ／\u00A0nebula");
    assertThat(noteTitle.getTitleAliases(), hasSize(2));
    assertThat(noteTitle.getTitleAliases().get(0).stem(), equalTo("nebulas"));
    assertThat(noteTitle.getTitleAliases().get(1).stem(), equalTo("nebula"));

    noteTitle = new NoteTitle("cat\u3000／\u3000kitten");
    assertThat(noteTitle.getTitleAliases(), hasSize(2));
    assertThat(noteTitle.getTitleAliases().get(0).stem(), equalTo("kitten"));
    assertThat(noteTitle.getTitleAliases().get(1).stem(), equalTo("cat"));

    noteTitle = new NoteTitle("word1\u2000／\u2001word2\u2002／\u2003word3");
    assertThat(noteTitle.getTitleAliases(), hasSize(3));
    assertThat(noteTitle.getTitleAliases().get(0).stem(), equalTo("word3"));
    assertThat(noteTitle.getTitleAliases().get(1).stem(), equalTo("word2"));
    assertThat(noteTitle.getTitleAliases().get(2).stem(), equalTo("word1"));
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
    assertThat(noteTitle.matches("color"), is(true));
  }
}
