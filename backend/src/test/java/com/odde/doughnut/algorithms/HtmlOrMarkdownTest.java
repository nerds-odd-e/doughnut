package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HtmlOrMarkdownTest {
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
    assertThat(new HtmlOrMarkdown(htmlOrText).isBlank(), is(shouldBeBlank));
  }

  @Test
  void emptyString() {
    assertThat(new HtmlOrMarkdown("").isBlank(), is(true));
  }

  @Test
  void replaceText_with_special_char() {
    String input = "<p>Some sample text</p>";
    HtmlOrMarkdown htmlOrMarkdown = new HtmlOrMarkdown(input);

    Function<String, String> callback = text -> text + "\\";

    assertThat(htmlOrMarkdown.replaceText(callback), containsString("<p>Some sample text\\</p>"));
  }
}
