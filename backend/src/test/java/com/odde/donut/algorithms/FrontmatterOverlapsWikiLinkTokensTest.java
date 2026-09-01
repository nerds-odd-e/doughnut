package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrontmatterOverlapsWikiLinkTokensTest {

  @Test
  void overlapWikiLinkTokensFromFrontmatter_returns_overlaps_list_tokens() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            overlaps:
              - "[[Other Note]]"
              - "[[Shared Notebook:Hue|display]]"
            """);

    assertThat(
        FrontmatterOverlaps.overlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[Other Note]]", "[[Shared Notebook:Hue|display]]")));
  }

  @Test
  void overlapWikiLinkTokens_ignores_wiki_links_under_aliases() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - "[[Legacy Partner]]"
            overlaps:
              - "[[New Partner]]"
            """);

    assertThat(
        FrontmatterOverlaps.overlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[New Partner]]")));
  }

  @Test
  void overlapWikiLinkTokens_wiki_in_aliases_alone_yields_no_tokens() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - "[[Shared]]"
            """);

    assertThat(FrontmatterOverlaps.overlapWikiLinkTokensFromFrontmatter(fm), equalTo(List.of()));
  }

  @Test
  void overlapWikiLinkTokens_ignoresFileLookingMarkdownListItem() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            overlaps:
              - "[Title](/Folder/Title.md)"
            """);

    assertThat(FrontmatterOverlaps.overlapWikiLinkTokensFromFrontmatter(fm), equalTo(List.of()));
  }

  @Test
  void overlapWikiLinkTokensFromNoteContent_reads_overlaps_only() {
    String content =
        """
        ---
        aliases:
          - color
          - "[[Legacy Partner]]"
        overlaps:
          - "[[Other Note]]"
        ---

        body
        """;

    assertThat(
        FrontmatterOverlaps.overlapWikiLinkTokensFromNoteContent(content),
        equalTo(List.of("[[Other Note]]")));
  }
}
