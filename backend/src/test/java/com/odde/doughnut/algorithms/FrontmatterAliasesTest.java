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
}
