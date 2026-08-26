package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
        FrontmatterAliases.authoredValidationErrorForNoteContent(content),
        equalTo(Optional.empty()));
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
        FrontmatterAliases.authoredValidationErrorForNoteContent(content),
        equalTo(Optional.empty()));
  }

  @Test
  void authoredValidationError_rejects_scalar_aliases_value() {
    String content = "---\naliases: color\n---\n";

    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }

  static Stream<String> invalidAliasListContents() {
    return Stream.of(
        """
        ---
        aliases:
          - "   "
        ---
        """,
        """
        ---
        aliases:
          - [oops]
        ---
        """,
        """
        ---
        aliases:
          - bad|alias
        ---
        """);
  }

  @ParameterizedTest
  @MethodSource("invalidAliasListContents")
  void authoredValidationError_rejects_blank_and_invalid_list_items(String content) {
    assertThat(
        FrontmatterAliases.authoredValidationErrorForNoteContent(content).orElseThrow(),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }
}
