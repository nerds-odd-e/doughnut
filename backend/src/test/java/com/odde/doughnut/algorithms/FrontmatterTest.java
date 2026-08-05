package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FrontmatterTest {

  @Test
  void getString_returns_scalar_strings_unchanged() {
    Frontmatter fm = Frontmatter.parse("title: Hello\nwikidata_id: Q42\n");

    assertThat(fm.getString("title"), equalTo(Optional.of("Hello")));
    assertThat(fm.getString("TITLE"), equalTo(Optional.of("Hello")));
    assertThat(fm.getString("wikidata_id"), equalTo(Optional.of("Q42")));
  }

  @Test
  void getString_normalizes_scalar_numbers_and_booleans_to_strings() {
    Frontmatter fm = Frontmatter.parse("count: 3\nactive: true\n");

    assertThat(fm.getString("count"), equalTo(Optional.of("3")));
    assertThat(fm.getString("active"), equalTo(Optional.of("true")));
  }

  @Test
  void getString_returns_empty_for_lists_maps_and_null_instead_of_toString() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            tags:
              - alpha
              - beta
            nested:
              child: value
            empty: null
            """);

    assertThat(fm.getString("tags"), equalTo(Optional.empty()));
    assertThat(fm.getString("nested"), equalTo(Optional.empty()));
    assertThat(fm.getString("empty"), equalTo(Optional.empty()));
  }

  @Test
  void getPropertyValue_returns_scalar_for_supported_scalars() {
    Frontmatter fm = Frontmatter.parse("title: Hello\ncount: 2\nactive: false\n");

    assertThat(
        fm.getPropertyValue("title"),
        equalTo(Optional.of(new FrontmatterPropertyValue.Scalar("Hello"))));
    assertThat(
        fm.getPropertyValue("count"),
        equalTo(Optional.of(new FrontmatterPropertyValue.Scalar("2"))));
    assertThat(
        fm.getPropertyValue("active"),
        equalTo(Optional.of(new FrontmatterPropertyValue.Scalar("false"))));
  }

  @Test
  void getPropertyValue_returns_list_items_in_yaml_order() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            tags:
              - alpha
              - beta
            flow: [one, two]
            """);

    assertThat(
        fm.getPropertyValue("tags"),
        equalTo(Optional.of(new FrontmatterPropertyValue.ListItems(List.of("alpha", "beta")))));
    assertThat(
        fm.getPropertyValue("flow"),
        equalTo(Optional.of(new FrontmatterPropertyValue.ListItems(List.of("one", "two")))));
  }

  @Test
  void getPropertyValue_normalizes_list_item_numbers_and_booleans() {
    Frontmatter fm = Frontmatter.parse("nums: [1, 2]\nflags:\n  - true\n  - false\n");

    assertThat(
        fm.getPropertyValue("nums"),
        equalTo(Optional.of(new FrontmatterPropertyValue.ListItems(List.of("1", "2")))));
    assertThat(
        fm.getPropertyValue("flags"),
        equalTo(Optional.of(new FrontmatterPropertyValue.ListItems(List.of("true", "false")))));
  }

  @Test
  void getPropertyValue_returns_empty_list_for_empty_yaml_sequence() {
    Frontmatter fm = Frontmatter.parse("tags: []\n");

    assertThat(
        fm.getPropertyValue("tags"),
        equalTo(Optional.of(new FrontmatterPropertyValue.ListItems(List.of()))));
  }

  @Test
  void getPropertyValue_returns_empty_for_unsupported_values() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            null_scalar: null
            nested_list:
              - [bad]
            map_value:
              child: x
            """);

    assertThat(fm.getPropertyValue("null_scalar"), equalTo(Optional.empty()));
    assertThat(fm.getPropertyValue("nested_list"), equalTo(Optional.empty()));
    assertThat(fm.getPropertyValue("map_value"), equalTo(Optional.empty()));
    assertThat(fm.getPropertyValue("missing"), equalTo(Optional.empty()));
  }

  @Test
  void valueStringsInInsertionOrder_scalars_only() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            first: one
            tags:
              - a
              - b
            second: 2
            nested:
              child: x
            """);

    assertThat(fm.stringValuesInInsertionOrder(), equalTo(List.of("one", "2")));
  }

  @Test
  void supportedValueStringsInInsertionOrder_includes_list_items() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            first: one
            tags:
              - a
              - b
            second: 2
            nested:
              child: x
            """);

    assertThat(fm.supportedValueStringsInInsertionOrder(), equalTo(List.of("one", "a", "b", "2")));
  }
}
