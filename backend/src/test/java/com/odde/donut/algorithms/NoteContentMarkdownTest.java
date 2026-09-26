package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
    assertThat(split.isPresent(), equalTo(true));
    assertThat(split.get().frontmatter().getString("key"), equalTo(Optional.of("v")));
    assertThat(split.get().body(), equalTo("Hello"));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_handles_incomplete_fence() {
    String s = "---\nno closing";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void bodyWithoutLeadingFrontmatter_preserves_crlf_when_no_frontmatter() {
    String s = "Line1\r\nLine2";
    assertThat(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(s), equalTo(s));
  }

  @Test
  void wikidataIdScalarFromLeadingFrontmatter_empty_when_no_frontmatter() {
    assertThat(
        NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter("plain"),
        equalTo(Optional.empty()));
  }

  @Test
  void wikidataIdScalarFromLeadingFrontmatter_reads_wikidata_id_scalar() {
    assertThat(
        NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter("---\nwikidata_id: Q99\n---\n"),
        equalTo(Optional.of("Q99")));
  }

  @Test
  void setLeadingFrontmatterProperty_appends_to_existing_frontmatter() {
    String content = "---\nsource: \"[[Moon]]\"\n---\nBody";

    String result =
        NoteContentMarkdown.setLeadingFrontmatterProperty(content, "a part of", "[[Earth]]");

    assertThat(
        result,
        equalTo(
            "---\n" + "source: \"[[Moon]]\"\n" + "a part of: '[[Earth]]'\n" + "---\n" + "Body"));
  }

  @Test
  void withNoteImageMask_replaces_only_the_mask_line() {
    String content =
        "---\n# a comment\nimage_mask: 1 1 2 2\ntags: [x, y]\ndescription: \"Quoted: value\"\n---\nBody";

    assertThat(
        NoteContentMarkdown.withNoteImageMask(content, "10 10 20 20"),
        equalTo(content.replace("image_mask: 1 1 2 2", "image_mask: 10 10 20 20")));
  }

  @Test
  void setLeadingFrontmatterProperty_creates_frontmatter_when_none() {
    String content = "Hello";

    String result =
        NoteContentMarkdown.setLeadingFrontmatterProperty(content, "a part of", "[[Earth]]");

    assertThat(result, equalTo("---\n" + "a part of: '[[Earth]]'\n" + "---\n" + "Hello"));
  }
}
