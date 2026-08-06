package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FrontmatterAliasesWikiLinkOverlapTest {

  @Test
  void fromFrontmatter_returns_only_plain_aliases_when_wiki_link_overlap_declared() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - "[[Other Note]]"
              - "[[Shared Notebook:Hue|display]]"
            """);

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color")));
  }

  @Test
  void fromFrontmatter_returns_empty_for_wiki_link_only_alias_list() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - "[[Other Note]]"
            """);

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of()));
  }

  @Test
  void fromFrontmatter_skips_wiki_link_items_alongside_invalid_plain_items() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - "[[Other Note]]"
              - bad|alias
              - good
            """);

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color", "good")));
  }

  @Test
  void overlapWikiLinkTokensFromFrontmatter_returns_wiki_link_tokens_in_order() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - "[[Other Note]]"
              - "[[Shared Notebook:Hue|display]]"
            """);

    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[Other Note]]", "[[Shared Notebook:Hue|display]]")));
  }

  @Test
  void overlapWikiLinkTokensFromFrontmatter_is_empty_when_aliases_absent() {
    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(Frontmatter.parse("color: red\n")),
        equalTo(List.of()));
  }

  @Test
  void overlapWikiLinkTokensFromFrontmatter_is_empty_for_empty_alias_list() {
    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(Frontmatter.parse("aliases: []\n")),
        equalTo(List.of()));
  }

  @Test
  void overlapWikiLinkTokensFromNoteContent_returns_authored_tokens() {
    String content =
        """
        ---
        aliases:
          - color
          - "[[Other Note]]"
        ---

        body
        """;

    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromNoteContent(content),
        equalTo(List.of("[[Other Note]]")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "[[Other Note]]",
        "[[Shared Notebook:Hue]]",
        "[[Title|display]]",
        "[[Shared Notebook:Hue|display]]",
        "[[",
        "see [[Other]]",
        "[[a]][[b]]",
        "[[]]"
      })
  void authoredValidationError_rejects_wiki_link_alias_items(String item) {
    String content =
        """
        ---
        aliases:
          - "%s"
        ---
        """
            .formatted(item);

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }
}
