package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TitleAliasMigrationPlanTest {

  @Test
  void primary_only_title_has_no_aliases_or_suffix_fragments() {
    var plan = TitleAliasMigrationPlan.from("colour");

    assertThat(plan.primary().stem(), equalTo("colour"));
    assertThat(plan.primary().suffixMarker(), is(false));
    assertThat(plan.plainAliases(), is(empty()));
    assertThat(plan.retainedSuffixFragments(), is(empty()));
    assertThat(plan.qualifier(), is(Optional.empty()));
  }

  @Test
  void splits_plain_aliases_after_primary() {
    var plan = TitleAliasMigrationPlan.from("colour／color");

    assertThat(plan.primary().stem(), equalTo("colour"));
    assertThat(plan.plainAliases(), contains("color"));
    assertThat(plan.retainedSuffixFragments(), is(empty()));
    assertThat(plan.qualifier(), is(Optional.empty()));
  }

  @Test
  void multiple_plain_aliases_preserve_segment_order() {
    var plan = TitleAliasMigrationPlan.from("colour／color／hue");

    assertThat(plan.primary().stem(), equalTo("colour"));
    assertThat(plan.plainAliases(), contains("color", "hue"));
  }

  @Test
  void escaped_fullwidth_slash_is_literal_inside_alias_segment() {
    var plan = TitleAliasMigrationPlan.from("colour／hue／／tone");

    assertThat(plan.primary().stem(), equalTo("colour"));
    assertThat(plan.plainAliases(), contains("hue／tone"));
    assertThat(plan.retainedSuffixFragments(), is(empty()));
  }

  @Test
  void escaped_fullwidth_slash_keeps_primary_only_title_intact() {
    var plan = TitleAliasMigrationPlan.from("cat／／kitten");

    assertThat(plan.primary().stem(), equalTo("cat／kitten"));
    assertThat(plan.plainAliases(), is(empty()));
  }

  @Test
  void ascii_slash_does_not_create_aliases() {
    var plan = TitleAliasMigrationPlan.from("cat / kitten");

    assertThat(plan.primary().stem(), equalTo("cat / kitten"));
    assertThat(plan.plainAliases(), is(empty()));
  }

  @Test
  void extracts_qualifier_without_splitting_on_fullwidth_slash() {
    var plan = TitleAliasMigrationPlan.from("cat (a／b)");

    assertThat(plan.primary().stem(), equalTo("cat"));
    assertThat(plan.plainAliases(), is(empty()));
    assertThat(plan.qualifier(), is(Optional.of("a／b")));
  }

  @Test
  void extracts_qualifier_with_plain_aliases() {
    var plan = TitleAliasMigrationPlan.from("cat／kitten (animal)");

    assertThat(plan.primary().stem(), equalTo("cat"));
    assertThat(plan.plainAliases(), contains("kitten"));
    assertThat(plan.qualifier(), is(Optional.of("animal")));
  }

  @Test
  void routes_tilde_segments_to_retained_suffix_fragments() {
    var plan = TitleAliasMigrationPlan.from("word／~suffix");

    assertThat(plan.primary().stem(), equalTo("word"));
    assertThat(plan.plainAliases(), is(empty()));
    assertThat(plan.retainedSuffixFragments(), contains("suffix"));
  }

  @Test
  void mixed_plain_and_tilde_segments() {
    var plan = TitleAliasMigrationPlan.from("word／~logical／alias");

    assertThat(plan.primary().stem(), equalTo("word"));
    assertThat(plan.plainAliases(), contains("alias"));
    assertThat(plan.retainedSuffixFragments(), contains("logical"));
  }

  @Test
  void primary_with_tilde_marker_is_not_a_plain_alias() {
    var plan = TitleAliasMigrationPlan.from("~logy／~logical");

    assertThat(plan.primary().suffixMarker(), is(true));
    assertThat(plan.primary().stem(), equalTo("logy"));
    assertThat(plan.plainAliases(), is(empty()));
    assertThat(plan.retainedSuffixFragments(), contains("logical"));
  }

  @Test
  void normalizes_unicode_whitespace_in_segments() {
    var plan = TitleAliasMigrationPlan.from("nebulas ／\u00A0nebula");

    assertThat(plan.primary().stem(), equalTo("nebulas"));
    assertThat(plan.plainAliases(), contains("nebula"));

    plan = TitleAliasMigrationPlan.from("cat\u3000／\u3000kitten");
    assertThat(plan.primary().stem(), equalTo("cat"));
    assertThat(plan.plainAliases(), contains("kitten"));
  }
}
