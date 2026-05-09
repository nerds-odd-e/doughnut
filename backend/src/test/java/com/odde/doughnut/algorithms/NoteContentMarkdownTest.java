package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
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

  @Test
  void wikidataIdScalarFromLeadingFrontmatter_empty_when_no_frontmatter() {
    assertTrue(NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter("plain").isEmpty());
  }

  @Test
  void wikidataIdScalarFromLeadingFrontmatter_reads_wikidata_id_scalar() {
    assertThat(
        NoteContentMarkdown.wikidataIdScalarFromLeadingFrontmatter("---\nwikidata_id: Q99\n---\n"),
        equalTo(Optional.of("Q99")));
  }

  @Test
  void
      removeWikiLinksFromLeadingFrontmatterProperties_removes_empty_property_and_keeps_body_link() {
    String content = "---\ntarget: \"[[Target]]\"\n---\nBody still links [[Target]]";

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Target")),
        equalTo(Optional.of("Body still links [[Target]]")));
  }

  @Test
  void removeWikiLinksFromLeadingFrontmatterProperties_removes_only_matching_property_link() {
    String content = "---\nsource: \"[[Source]]\"\ntarget: \"[[Target]]\"\n---\nBody";

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Target")),
        equalTo(Optional.of("---\nsource: \"[[Source]]\"\n---\nBody")));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_prepends_frontmatter_when_none() {
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
            "Hello", true, "/attachments/images/9/a.png", ""),
        equalTo("---\nimage: /attachments/images/9/a.png\n---\nHello"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_adds_mask_when_present() {
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
            "Body", true, "/attachments/images/1/x.jpg", "0 0 10 10"),
        equalTo(
            "---\n"
                + "image: /attachments/images/1/x.jpg\n"
                + "image_mask: \"0 0 10 10\"\n"
                + "---\n"
                + "Body"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_replaces_existing_image_lines() {
    String in = "---\nimage: /old\nwikidata_id: Q1\n---\nB";
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
            in, true, "/attachments/images/2/n.png", ""),
        equalTo("---\nwikidata_id: Q1\nimage: /attachments/images/2/n.png\n---\nB"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_removes_image_when_hasImage_false() {
    String in = "---\nimage: /x\nimage_mask: 1 2 3 4\n---\nBody";
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(in, false, "", ""), equalTo("Body"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_quotes_url_with_colon() {
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
            "Hi", true, "https://example.com/a.png", ""),
        equalTo("---\n" + "image: \"https://example.com/a.png\"\n" + "---\n" + "Hi"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_plain_body_unchanged_when_clearing_without_frontmatter() {
    String plain = "No frontmatter";
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(plain, false, "", ""), equalTo(plain));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_clears_image_but_keeps_other_frontmatter() {
    String in = "---\nimage: /x\ntopic: t\n---\nBody";
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(in, false, "", ""),
        equalTo("---\ntopic: t\n---\nBody"));
  }
}
