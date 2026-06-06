package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyKeyNamingTest {

  @Test
  void propertyKeyBaseAndSuffix_parses_bare_and_suffixed_keys() {
    assertThat(
        PropertyKeyNaming.propertyKeyBaseAndSuffix("url"),
        equalTo(new PropertyKeyNaming.BaseAndSuffix("url", null)));
    assertThat(
        PropertyKeyNaming.propertyKeyBaseAndSuffix("url 2"),
        equalTo(new PropertyKeyNaming.BaseAndSuffix("url", 2)));
    assertThat(
        PropertyKeyNaming.propertyKeyBaseAndSuffix("example of 2"),
        equalTo(new PropertyKeyNaming.BaseAndSuffix("example of", 2)));
  }

  @Test
  void nextAvailablePropertyKeyForBase_returns_base_when_family_unused() {
    assertThat(
        PropertyKeyNaming.nextAvailablePropertyKeyForBase("a part of", List.of()),
        equalTo("a part of"));
  }

  @Test
  void nextAvailablePropertyKeyForBase_returns_next_suffixed_key_when_base_taken() {
    assertThat(
        PropertyKeyNaming.nextAvailablePropertyKeyForBase("a part of", List.of("a part of")),
        equalTo("a part of 2"));
    assertThat(
        PropertyKeyNaming.nextAvailablePropertyKeyForBase(
            "a part of", List.of("a part of", "a part of 2")),
        equalTo("a part of 3"));
  }

  @Test
  void nextAvailablePropertyKeyForBase_treats_existing_key_case_insensitively() {
    assertThat(
        PropertyKeyNaming.nextAvailablePropertyKeyForBase("a part of", List.of("A part of")),
        equalTo("a part of 2"));
  }
}
