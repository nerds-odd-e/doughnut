package com.odde.donut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DisplayNamePathSeparatorsTrimTest {

  @Test
  void nullReturnsNull() {
    assertThat(DisplayNamePathSeparators.trimSurroundingWhitespace(null), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "\t", "\r", "\n", "\r\n", "\u3000", "\u00A0", "\u200B", "\uFEFF"})
  void trimsLeadingAndTrailingWhitespace(String ws) {
    assertThat(
        DisplayNamePathSeparators.trimSurroundingWhitespace(ws + "hello" + ws), equalTo("hello"));
  }

  @Test
  void preservesInternalWhitespace() {
    assertThat(
        DisplayNamePathSeparators.trimSurroundingWhitespace(" cat \u3000 dog "),
        equalTo("cat \u3000 dog"));
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "\t", "\r\n", "\u3000", "\u00A0", "\u200B", "\uFEFF"})
  void allWhitespaceBecomesEmpty(String ws) {
    assertThat(DisplayNamePathSeparators.trimSurroundingWhitespace(ws), equalTo(""));
  }

  @Test
  void normalizeDisplayNameTrimsAndConvertsPathSeparators() {
    assertThat(
        DisplayNamePathSeparators.normalizeDisplayName(" \u3000foo/bar: baz\u3000 "),
        equalTo("foo／bar： baz"));
  }

  @Test
  void normalizeDisplayNameConvertsOsInvalidCharacters() {
    assertThat(
        DisplayNamePathSeparators.normalizeDisplayName("Recipe*?\"<>|"), equalTo("Recipe＊？＂＜＞｜"));
  }

  @Test
  void normalizeDisplayNameConvertsAsciiControlsToSpaceThenTrims() {
    assertThat(
        DisplayNamePathSeparators.normalizeDisplayName("\u0001Recipe\u0007*\u001F"),
        equalTo("Recipe ＊"));
  }
}
