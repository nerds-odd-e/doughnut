package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrontmatterAliasesTest {

  @Test
  void fromFrontmatter_returns_valid_yaml_list_items() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - hue
            """);

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color", "hue")));
  }

  @Test
  void fromFrontmatter_ignores_scalar_aliases_value() {
    Frontmatter fm = Frontmatter.parse("aliases: color\n");

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of()));
  }

  @Test
  void fromFrontmatter_skips_blank_and_invalid_items_in_list() {
    Frontmatter fm =
        Frontmatter.parse(
            """
            aliases:
              - color
              - "   "
              - bad|alias
              - [oops]
              - good
            """);

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color", "good")));
  }

  @Test
  void fromFrontmatter_trims_surrounding_whitespace() {
    Frontmatter fm = Frontmatter.parse("aliases:\n  - '  color  '\n");

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("color")));
  }

  @Test
  void fromFrontmatter_dedupes_by_normalized_key() {
    Frontmatter fm = Frontmatter.parse("aliases:\n  - Color\n  - color\n");

    assertThat(FrontmatterAliases.fromFrontmatter(fm), equalTo(List.of("Color")));
  }

  @Test
  void authoredValidationError_isEmpty_when_aliases_property_absent() {
    String content =
        """
        ---
        color: red
        ---

        body
        """;

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).isPresent(),
        equalTo(false));
  }

  @Test
  void authoredValidationError_isEmpty_for_valid_alias_list() {
    String content =
        """
        ---
        aliases:
          - color
          - hue
        ---

        body
        """;

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).isPresent(),
        equalTo(false));
  }

  @Test
  void authoredValidationError_rejects_scalar_aliases_value() {
    String content = "---\naliases: color\n---\n";

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }

  @Test
  void authoredValidationError_rejects_blank_and_invalid_list_items() {
    String blankItem =
        """
        ---
        aliases:
          - "   "
        ---
        """;
    String nestedItem =
        """
        ---
        aliases:
          - [oops]
        ---
        """;
    String pipeItem =
        """
        ---
        aliases:
          - bad|alias
        ---
        """;

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(blankItem).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(nestedItem).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(pipeItem).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }

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
  void overlapWikiLinkTokensFromFrontmatter_is_empty_when_aliases_absent_or_empty() {
    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(Frontmatter.parse("color: red\n")),
        equalTo(List.of()));
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

  @Test
  void authoredValidationError_accepts_well_formed_wiki_link_alias_items() {
    String content =
        """
        ---
        aliases:
          - color
          - "[[Other Note]]"
          - "[[Shared Notebook:Hue]]"
          - "[[Title|display]]"
          - "[[Shared Notebook:Hue|display]]"
        ---

        body
        """;

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).isPresent(),
        equalTo(false));
  }

  @Test
  void authoredValidationError_rejects_embedded_or_malformed_wiki_link_items() {
    for (String item : List.of("[[", "see [[Other]]", "[[a]][[b]]", "[[]]")) {
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
    assertThat(
        FrontmatterAliases.overlapWikiLinkTokensFromFrontmatter(fm),
        equalTo(List.of("[[Other Note]]")));
  }
}
