package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecallTitleSegmentsTest {

  @Test
  void primary_only_title_has_no_suffix_fragments() {
    var segments = RecallTitleSegments.from("colour");

    assertThat(segments.primary().stem(), equalTo("colour"));
    assertThat(segments.primary().suffixMarker(), is(false));
    assertThat(segments.retainedSuffixFragments(), is(empty()));
  }

  @ParameterizedTest
  @CsvSource({
    "colour／color, colour／color",
    "cat／／kitten, cat／／kitten",
    "cat / kitten, cat / kitten",
  })
  void literal_slash_stays_in_primary_title(String title, String expectedPrimary) {
    assertThat(RecallTitleSegments.from(title).primary().stem(), equalTo(expectedPrimary));
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

  @ParameterizedTest
  @CsvSource({
    "nebulas ／\u00A0nebula, nebulas ／ nebula",
    "cat\u3000／\u3000kitten, cat ／ kitten",
  })
  void normalizes_unicode_whitespace_in_segments(String title, String expectedPrimary) {
    assertThat(RecallTitleSegments.from(title).primary().stem(), equalTo(expectedPrimary));
  }
}
