package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class NoteContentTitleHeadingTest {

  @Test
  void nullContentUnchanged() {
    assertThat(NoteContentTitleHeading.withoutRepeatedTitleHeading("Title", null), nullValue());
  }

  @Test
  void stripsLeadingAtxHeadingMatchingTitle() {
    assertThat(
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            "Key Suggestion", "# Key Suggestion\n\nBody"),
        equalTo("Body"));
  }

  @Test
  void stripsLeadingH2MatchingTitleIgnoringCase() {
    assertThat(
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            "Key Suggestion", "## key suggestion\nBody"),
        equalTo("Body"));
  }

  @Test
  void keepsLeadingHeadingThatDoesNotMatchTitle() {
    String content = "# Other Heading\n\nBody";
    assertThat(
        NoteContentTitleHeading.withoutRepeatedTitleHeading("Key Suggestion", content),
        equalTo(content));
  }

  @Test
  void keepsBodyWithoutHeading() {
    assertThat(
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            "Key Suggestion", "Body without heading"),
        equalTo("Body without heading"));
  }
}
