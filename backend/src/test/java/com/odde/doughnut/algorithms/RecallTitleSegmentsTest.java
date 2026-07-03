package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class RecallTitleSegmentsTest {

  @Test
  void primary_only_title_has_no_suffix_fragments() {
    var segments = RecallTitleSegments.from("colour");

    assertThat(segments.primary().stem(), equalTo("colour"));
    assertThat(segments.primary().suffixMarker(), is(false));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void fullwidth_slash_text_stays_in_primary_title() {
    var segments = RecallTitleSegments.from("colour／color");

    assertThat(segments.primary().stem(), equalTo("colour／color"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void repeated_fullwidth_slash_stays_in_primary_title() {
    var segments = RecallTitleSegments.from("cat／／kitten");

    assertThat(segments.primary().stem(), equalTo("cat／／kitten"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void ascii_slash_stays_in_primary_title() {
    var segments = RecallTitleSegments.from("cat / kitten");

    assertThat(segments.primary().stem(), equalTo("cat / kitten"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void qualifier_does_not_split_primary_on_fullwidth_slash() {
    var segments = RecallTitleSegments.from("cat (a／b)");

    assertThat(segments.primary().stem(), equalTo("cat"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void routes_tilde_segments_to_retained_suffix_fragments() {
    var segments = RecallTitleSegments.from("word／~suffix");

    assertThat(segments.primary().stem(), equalTo("word"));
    assertThat(segments.retainedSuffixFragments(), contains("suffix"));
  }

  @Test
  void mixed_plain_and_tilde_segments() {
    var segments = RecallTitleSegments.from("word／~logical／alias");

    assertThat(segments.primary().stem(), equalTo("word／alias"));
    assertThat(segments.retainedSuffixFragments(), contains("logical"));
  }

  @Test
  void primary_keeps_its_tilde_marker() {
    var segments = RecallTitleSegments.from("~logy／~logical");

    assertThat(segments.primary().suffixMarker(), is(true));
    assertThat(segments.primary().stem(), equalTo("logy"));
    assertThat(segments.retainedSuffixFragments(), contains("logical"));
  }

  @Test
  void normalizes_unicode_whitespace_in_segments() {
    var segments = RecallTitleSegments.from("nebulas ／\u00A0nebula");

    assertThat(segments.primary().stem(), equalTo("nebulas ／ nebula"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));

    segments = RecallTitleSegments.from("cat\u3000／\u3000kitten");
    assertThat(segments.primary().stem(), equalTo("cat ／ kitten"));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }
}
