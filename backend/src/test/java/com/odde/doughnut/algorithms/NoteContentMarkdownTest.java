package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class NoteContentMarkdownTest {

  @Test
  void bodyWithoutLeadingFrontmatter_handles_null_and_empty() {
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(null), nullValue());
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(""), equalTo(""));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_returns_plain_when_no_frontmatter() {
    String s = "Hello\nWorld";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_strips_frontmatter() {
    assertThat(
        NoteContentMarkdown.bodyWithoutLeadingFrontmatter("---\nkey: v\n---\nHello"),
        equalTo("Hello"));
  }

  @Test
  void splitLeadingFrontmatter_parses_yaml_and_body() {
    Optional<NoteContentMarkdown.LeadingFrontmatter> split =
        NoteContentMarkdown.splitLeadingFrontmatter("---\nkey: v\n---\nHello");
    assertTrue(split.isPresent());
    assertThat(split.get().yamlRaw(), equalTo("key: v"));
    assertThat(split.get().body(), equalTo("Hello"));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_handles_incomplete_fence() {
    String s = "---\nno closing";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_handles_body_after_frontmatter() {
    String in = "---\na: 1\n---\nBody text";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(in), equalTo("Body text"));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_preserves_crlf_when_no_frontmatter() {
    String s = "Line1\r\nLine2";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }
}
