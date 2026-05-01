package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoteDetailsMarkdownTest {

  @Test
  void nullAndEmptyUnchanged() {
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(null), nullValue());
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(""), equalTo(""));
  }

  @Test
  void noFrontmatterUnchanged_preservesCrLf() {
    String s = "Hello\r\nWorld";
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void stripsWellFormedFence() {
    assertThat(
        NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter("---\nkey: v\n---\nHello"),
        equalTo("Hello"));
  }

  @Test
  void splitsFrontmatterAndBody() {
    Optional<NoteDetailsMarkdown.LeadingFrontmatter> split =
        NoteDetailsMarkdown.splitLeadingFrontmatter("---\nkey: v\n---\nHello");

    assertThat(split.orElseThrow().yamlRaw(), equalTo("key: v"));
    assertThat(split.orElseThrow().body(), equalTo("Hello"));
  }

  @Test
  void unclosedOpeningFenceReturnsOriginal() {
    String s = "---\nkey: v\nHello";
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void bomThenFrontmatterStripsFence() {
    String in = "\uFEFF---\nkey: v\n---\nBody text";
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(in), equalTo("Body text"));
  }

  @Test
  void firstLineMustMatchFenceExactly() {
    String s = " ---\nkey: v\n---\n";
    assertThat(NoteDetailsMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }
}
