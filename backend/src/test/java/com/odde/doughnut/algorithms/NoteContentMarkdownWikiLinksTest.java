package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NoteContentMarkdownWikiLinksTest {

  private static final String NESTED_AND_LISTED_FRONTMATTER =
      """
      nested:
        child: "[[Nested]]"
      listed:
        - "[[Listed]]"
      """;

  @Test
  void wikiLinkInnersInOccurrenceOrder_readsWikiLinkFromParsedFrontmatterScalar() {
    String title = "In volitional (\"let's\" or \"I shall\") statements";
    String content = Frontmatter.empty().set("example of", "[[" + title + "]]").fenced("");

    assertThat(
        NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content), equalTo(List.of(title)));
  }

  @Test
  void wikiLinkInnersInOccurrenceOrder_readsWikiLinksFromListItemsInYamlOrder() {
    String content =
        Frontmatter.parse(
                """
                example of:
                  - "[[First]]"
                  - plain
                  - "[[Second]]"
                """)
            .fenced("");

    assertThat(
        NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content),
        equalTo(List.of("First", "Second")));
  }

  @Test
  void wikiLinkInnersInOccurrenceOrder_scansScalarsThenListItemsInPropertyOrder() {
    String content =
        Frontmatter.parse(
                """
                scalar: "[[Scalar]]"
                listed:
                  - "[[One]]"
                  - "[[Two]]"
                """)
            .fenced("Body [[Body]]");

    assertThat(
        NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content),
        equalTo(List.of("Scalar", "One", "Two", "Body")));
  }

  @Test
  void wikiLinkInnersInOccurrenceOrder_skipsUnsupportedNestedValues() {
    String content = Frontmatter.parse(NESTED_AND_LISTED_FRONTMATTER).fenced("");

    assertThat(
        NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content), equalTo(List.of("Listed")));
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
  void removeWikiLinksFromLeadingFrontmatterProperties_updatesListItemsNotCollectionString() {
    String content =
        Frontmatter.parse(
                """
                example of:
                  - "[[Target]]"
                  - keep
                """)
            .fenced("Body");

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Target")),
        equalTo(Optional.of(Frontmatter.parse("example of:\n  - keep\n").fenced("Body"))));
  }

  @Test
  void removeWikiLinksFromLeadingFrontmatterProperties_dropsBlankListItemAfterRemoval() {
    String content =
        Frontmatter.parse(
                """
                example of:
                  - "[[Only]]"
                  - keep
                """)
            .fenced("");

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Only")),
        equalTo(Optional.of(Frontmatter.parse("example of:\n  - keep\n").fenced(""))));
  }

  @Test
  void removeWikiLinksFromLeadingFrontmatterProperties_removesPropertyWhenAllListItemsBlank() {
    String content = Frontmatter.parse("example of:\n  - \"[[Only]]\"\n").fenced("Body");

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Only")),
        equalTo(Optional.of("Body")));
  }

  @Test
  void removeWikiLinksFromLeadingFrontmatterProperties_leavesUnsupportedNestedValuesUnchanged() {
    String content = Frontmatter.parse(NESTED_AND_LISTED_FRONTMATTER).fenced("");

    assertThat(
        NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
            content, Set.of("Listed")),
        equalTo(
            Optional.of(
                Frontmatter.parse(
                        """
                        nested:
                          child: "[[Nested]]"
                        """)
                    .fenced(""))));
  }
}
