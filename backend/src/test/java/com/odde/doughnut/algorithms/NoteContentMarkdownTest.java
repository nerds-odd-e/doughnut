package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class NoteContentMarkdownTest {

  static Stream<Arguments> referencedImagePaths() {
    return Stream.of(
        Arguments.of("---\nimage: /attachments/images/12/a.png\n---\n", 12),
        Arguments.of("---\nIMAGE: /attachments/images/3/z.png\n---\n", 3),
        Arguments.of("---\nimage: \"/attachments/images/9/x.png\"\n---\n", 9),
        Arguments.of("---\nimage: '/attachments/images/8/y.png'\n---\n", 8));
  }

  @ParameterizedTest
  @MethodSource("referencedImagePaths")
  void leadingFrontmatterImageReference_referenced_for_canonical_paths(
      String content, int expectedId) {
    NoteContentMarkdown.LeadingFrontmatterImageReference ref =
        NoteContentMarkdown.leadingFrontmatterImageReference(content);
    assertThat(
        ref,
        equalTo(new NoteContentMarkdown.LeadingFrontmatterImageReference.Referenced(expectedId)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plain",
        "---\na: 1\n---\nBody",
        "---\nimage:\n---\nBody",
        "---\nimage: ''\n---\nBody",
        "---\nimage: \"\"\n---\nBody",
      })
  void leadingFrontmatterImageReference_none_when_no_usable_image_scalar(String content) {
    NoteContentMarkdown.LeadingFrontmatterImageReference ref =
        NoteContentMarkdown.leadingFrontmatterImageReference(content);
    assertThat(ref, equalTo(new NoteContentMarkdown.LeadingFrontmatterImageReference.None()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "---\nimage: https://example.com/a.png\n---\n",
        "---\nimage: /wrong/prefix/1/x.png\n---\n",
        "---\nimage: attachments/images/1/x.png\n---\n",
      })
  void leadingFrontmatterImageReference_invalid_skips_cleanup_when_scalar_not_attachment_path(
      String content) {
    NoteContentMarkdown.LeadingFrontmatterImageReference ref =
        NoteContentMarkdown.leadingFrontmatterImageReference(content);
    assertThat(
        ref,
        equalTo(new NoteContentMarkdown.LeadingFrontmatterImageReference.InvalidPathPresent()));
  }

  @Test
  void attachmentImageIdFromPath_empty_for_non_matching_paths() {
    assertTrue(NoteContentMarkdown.attachmentImageIdFromPath("").isEmpty());
    assertTrue(NoteContentMarkdown.attachmentImageIdFromPath("/attachments/images//x").isEmpty());
    assertTrue(NoteContentMarkdown.attachmentImageIdFromPath("/attachments/image/1/x").isEmpty());
  }

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
    assertThat(split.get().frontmatter().getString("key"), equalTo(Optional.of("v")));
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
  void wikiLinkInnersInOccurrenceOrder_readsWikiLinkFromParsedFrontmatterScalar() {
    String title = "In volitional (\"let's\" or \"I shall\") statements";
    String content = Frontmatter.empty().set("example of", "[[" + title + "]]").fenced("");

    assertThat(
        NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content), equalTo(List.of(title)));
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
        equalTo(Optional.of("---\nsource: '[[Source]]'\n---\nBody")));
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
                + "image_mask: 0 0 10 10\n"
                + "---\n"
                + "Body"));
  }

  static Stream<Arguments> existingFrontmatterImageUpdates() {
    return Stream.of(
        Arguments.of(
            "---\nimage: /old\nwikidata_id: Q1\n---\nB",
            true,
            "/attachments/images/2/n.png",
            "",
            "---\nwikidata_id: Q1\nimage: /attachments/images/2/n.png\n---\nB"),
        Arguments.of(
            "---\nimage: /x\ntopic: t\n---\nBody", false, "", "", "---\ntopic: t\n---\nBody"),
        Arguments.of("---\nimage: /x\nimage_mask: 1 2 3 4\n---\nBody", false, "", "", "Body"));
  }

  @ParameterizedTest
  @MethodSource("existingFrontmatterImageUpdates")
  void mergeNoteImageScalarsIntoContent_updates_existing_frontmatter(
      String input, boolean hasImage, String imageUrl, String imageMask, String expected) {
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(input, hasImage, imageUrl, imageMask),
        equalTo(expected));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_url_with_colon_is_plain() {
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(
            "Hi", true, "https://example.com/a.png", ""),
        equalTo("---\n" + "image: https://example.com/a.png\n" + "---\n" + "Hi"));
  }

  @Test
  void mergeNoteImageScalarsIntoContent_plain_body_unchanged_when_clearing_without_frontmatter() {
    String plain = "No frontmatter";
    assertThat(
        NoteContentMarkdown.mergeNoteImageScalarsIntoContent(plain, false, "", ""), equalTo(plain));
  }

  @Test
  void addPropertyToLeadingFrontmatter_appends_to_existing_frontmatter() {
    String content = "---\nsource: \"[[Moon]]\"\n---\nBody";

    String result =
        NoteContentMarkdown.addPropertyToLeadingFrontmatter(content, "a part of", "[[Earth]]");

    assertThat(
        result,
        equalTo("---\n" + "source: '[[Moon]]'\n" + "a part of: '[[Earth]]'\n" + "---\n" + "Body"));
  }

  @Test
  void addPropertyToLeadingFrontmatter_creates_frontmatter_when_none() {
    String content = "Hello";

    String result =
        NoteContentMarkdown.addPropertyToLeadingFrontmatter(content, "a part of", "[[Earth]]");

    assertThat(result, equalTo("---\n" + "a part of: '[[Earth]]'\n" + "---\n" + "Hello"));
  }
}
