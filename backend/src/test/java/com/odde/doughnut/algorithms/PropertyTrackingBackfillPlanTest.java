package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PropertyTrackingBackfillPlanTest {

  @Test
  void forNote_excludes_reserved_keys_from_both_sets() {
    PropertyTrackingBackfillPlan.Result result =
        PropertyTrackingBackfillPlan.forNote(
            Frontmatter.parse(
                "topic: physics\nimage: /x\nurl 2: https://example.com\nwikidataId: Q1\nexample of: \"[[A]]\""),
            Set.of());

    assertThat(result.keysToIndex(), containsInAnyOrder("topic", "example of"));
    assertThat(result.keysToSeedSkipped(), containsInAnyOrder("topic"));
  }

  @Test
  void forNote_indexes_example_of_family_but_never_skip_seeds_them() {
    PropertyTrackingBackfillPlan.Result result =
        PropertyTrackingBackfillPlan.forNote(
            Frontmatter.parse("example of: \"[[A]]\"\nExample Of 2: \"[[B]]\"\ntopic: physics"),
            Set.of());

    assertThat(result.keysToIndex(), containsInAnyOrder("example of", "Example Of 2", "topic"));
    assertThat(result.keysToSeedSkipped(), containsInAnyOrder("topic"));
  }

  @Test
  void forNote_skips_seed_when_property_key_already_tracked_case_insensitively() {
    PropertyTrackingBackfillPlan.Result result =
        PropertyTrackingBackfillPlan.forNote(
            Frontmatter.parse("topic: physics\ndefinition: term"), Set.of("TOPIC"));

    assertThat(result.keysToIndex(), containsInAnyOrder("topic", "definition"));
    assertThat(result.keysToSeedSkipped(), containsInAnyOrder("definition"));
  }

  @Test
  void forNote_returns_empty_sets_for_empty_frontmatter() {
    PropertyTrackingBackfillPlan.Result result =
        PropertyTrackingBackfillPlan.forNote(Frontmatter.empty(), Set.of("topic"));

    assertThat(result.keysToIndex(), is(empty()));
    assertThat(result.keysToSeedSkipped(), is(empty()));
  }

  @Test
  void forNote_excludes_passthrough_keys_and_empty_lists() {
    PropertyTrackingBackfillPlan.Result result =
        PropertyTrackingBackfillPlan.forNote(
            Frontmatter.parse("tags:\n  - t1\naliases: []\ntopic: physics\nstatus: []"), Set.of());

    assertThat(result.keysToIndex(), containsInAnyOrder("topic"));
    assertThat(result.keysToSeedSkipped(), containsInAnyOrder("topic"));
  }
}
