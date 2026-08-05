package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrontmatterOverlapsGradingTokensTest {

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
  void gradingOverlapWikiLinkTokens_unions_overlaps_and_legacy_aliases_wiki_links() {
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
        FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[New Partner]]", "[[Legacy Partner]]")));
  }

  @Test
  void gradingOverlapWikiLinkTokens_dedupes_normalized_tokens_across_keys() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - "[[Shared]]"
            overlaps:
              - "[[shared]]"
            """);

    assertThat(
        FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[shared]]")));
  }

  @Test
  void gradingOverlapWikiLinkTokensFromNoteContent_reads_overlaps_only_when_aliases_plain() {
    String content =
        """
        ---
        aliases:
          - color
        overlaps:
          - "[[Other Note]]"
        ---

        body
        """;

    assertThat(
        FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromNoteContent(content),
        equalTo(List.of("[[Other Note]]")));
  }
}
