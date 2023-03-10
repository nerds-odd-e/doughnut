package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HtmlOrTextTest {
  @ParameterizedTest
  @CsvSource({
    "a, false",
    ", true",
    "<p> </p>, true",
    "<br>, true",
    "<br  />, true",
    "<p><br></p>, true",
  })
  void is_blank(String htmlOrText, boolean shouldBeBlank) {
    assertThat(new HtmlOrText(htmlOrText).isBlank(), is(shouldBeBlank));
  }

  @Test
  void emptyString() {
    assertThat(new HtmlOrText("").isBlank(), is(true));
  }
}
