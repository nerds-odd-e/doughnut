package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

  @ParameterizedTest
  @CsvSource({
    "image, true",
    "Image, true",
    "image 2, true",
    "image_mask, true",
    "imageMask, true",
    "wikidata_id, true",
    "wikidataId, true",
    "url, true",
    "URL 2, true",
    "title_pattern, true",
    "titlePattern, true",
    "question_generation_instruction, true",
    "questionGenerationInstruction, true",
    "example of, false",
    "example of 2, false",
    "topic, false",
    "a part of, false",
  })
  void isReservedStructuralKey_matches_structural_keys_only(String key, boolean reserved) {
    assertThat(PropertyKeyNaming.isReservedStructuralKey(key), is(reserved));
  }

  @Test
  void isImagePropertyKey_does_not_match_image_mask() {
    assertThat(PropertyKeyNaming.isImagePropertyKey("image_mask"), is(false));
    assertThat(PropertyKeyNaming.isImageMaskPropertyKey("image_mask"), is(true));
  }

  @ParameterizedTest
  @CsvSource({
    "example of, true",
    "Example Of 2, true",
    "example, false",
    "topic, false",
  })
  void isExampleOfFamily_matches_example_of_family_only(String key, boolean exampleOf) {
    assertThat(PropertyKeyNaming.isExampleOfFamily(key), is(exampleOf));
  }
}
