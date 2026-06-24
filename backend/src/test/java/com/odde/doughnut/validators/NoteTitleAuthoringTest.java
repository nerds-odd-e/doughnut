package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoteTitleAuthoringTest {

  @ParameterizedTest
  @ValueSource(strings = {"colour／color", "cat／kitten (animal)", "word／alias"})
  void detectsPlainTitleAliasSegments(String title) {
    assertThat(NoteTitleAuthoring.hasPlainTitleAliasSegments(title), is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "colour",
        "word／~suffix",
        "~logy／~logical",
        "cat／／kitten",
        "TCP／／IP： Overview",
        "foo／／bar： baz"
      })
  void allowsTitlesWithoutPlainAliasSegments(String title) {
    assertThat(NoteTitleAuthoring.hasPlainTitleAliasSegments(title), is(false));
  }

  @Test
  void nullTitleIsAllowed() {
    assertThat(NoteTitleAuthoring.hasPlainTitleAliasSegments(null), is(false));
  }
}
