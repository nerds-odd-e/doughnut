package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoteIdUrlTest {

  @Test
  void noteIdFromRootRelativeHref_acceptsCanonicalCompactPath() {
    assertThat(NoteIdUrl.noteIdFromRootRelativeHref("/n1234"), equalTo(Optional.of(1234)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/n/1234",
        "/n1234/",
        "/n1234/p/topic",
        "/n1234?x=1",
        "/n1234#frag",
        "n1234",
        "/N1234",
        "/n",
        "https://doughnut.odd-e.com/n19921",
        "/Folder/Title.md"
      })
  void noteIdFromRootRelativeHref_rejectsNonCanonicalForms(String href) {
    assertThat(NoteIdUrl.noteIdFromRootRelativeHref(href), is(Optional.empty()));
  }

  @Test
  void tryParseAuthoredMarkdownLink_rebuildsNoteIdUrlTarget() {
    AuthoredNoteReference.NoteIdUrlTarget url =
        NoteIdUrl.tryParseAuthoredMarkdownLink("[wrong title](/n42)").orElseThrow();
    assertThat(url.authoredLink(), equalTo("[wrong title](/n42)"));
    assertThat(url.noteId(), equalTo(42));
    assertThat(url.href(), equalTo("/n42"));
    assertThat(url.displayText(), equalTo("wrong title"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Target", "[[Target]]", "[Target](/n/9)", "[Target](/n9/p/x)"})
  void tryParseAuthoredMarkdownLink_rejectsNonNoteIdUrlAuthoredLinks(String authored) {
    assertThat(NoteIdUrl.tryParseAuthoredMarkdownLink(authored), is(Optional.empty()));
  }
}
