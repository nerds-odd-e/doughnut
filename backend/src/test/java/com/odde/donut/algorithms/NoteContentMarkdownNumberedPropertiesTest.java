package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.PropertyFocus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NoteContentMarkdownNumberedPropertiesTest {
  @Test
  void consolidatesScalarFamiliesWithExactFocusMappingsAndUntouchedBytes() {
    String original =
        "\uFEFF---\r\n# keep 🥯\r\nexample of: '[[run]]'\r\n"
            + "unrelated: {a: 'quoted'} # untouched\r\nexample of 2: \"[[past tense]]\"\r\n"
            + "---\r\nBody  \r\n";
    var result = NoteContentMarkdown.consolidateNumberedProperties(original);
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        result.content(),
        equalTo(
            original
                .replace("example of: '[[run]]'", "example of: [\"[[run]]\", \"[[past tense]]\"]")
                .replace("example of 2: \"[[past tense]]\"\r\n", "")));
    assertThat(
        result.focuses(),
        equalTo(
            Map.of(
                new PropertyFocus("example of", ""), new PropertyFocus("example of", "[[run]]"),
                new PropertyFocus("example of 2", ""),
                    new PropertyFocus("example of", "[[past tense]]"))));
  }

  @Test
  void ordersSparseSourcesAndKeepsFirstEqualValueAndListItemFocus() {
    var result =
        NoteContentMarkdown.consolidateNumberedProperties(
            "---\ntopic 10: [C, B]\ntopic:\n  - A\n  - B\ntopic 2: B\nother: keep\n---\nBody");
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        result.content(), equalTo("---\ntopic: [\"A\", \"B\", \"C\"]\nother: keep\n---\nBody"));
    assertThat(
        result.focuses().get(new PropertyFocus("topic 10", "C")),
        equalTo(new PropertyFocus("topic", "C")));
    assertThat(
        result.focuses().get(new PropertyFocus("topic", "B")),
        equalTo(new PropertyFocus("topic", "B")));
  }

  @Test
  void createsMissingBaseAsListAndKeepsAuthoredFamiliesSeparate() {
    var result =
        NoteContentMarkdown.consolidateNumberedProperties(
            "---\nTopic 2: Upper\ntopic 2: lower\nurl 2: https://example.com\nimage 2: img\n"
                + "type 2: Relationship\ntopic two: word\n---\n");
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        result.content(),
        equalTo(
            "---\nTopic: [\"Upper\"]\ntopic: [\"lower\"]\n"
                + "url: [\"https://example.com\"]\nimage 2: img\ntype 2: Relationship\ntopic two: word\n---\n"));
  }

  @Test
  void preservesExactWhitespaceFamiliesAndTrailingWhitespaceKeys() {
    var result =
        NoteContentMarkdown.consolidateNumberedProperties(
            "---\n' topic': leading\n' topic 2': next\ntopic: plain\ntopic 2: second\n"
                + "'topic ': trailing\n' topic  2': doubled\n' topic 2 ': untouched\n---\n");
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        result.content(),
        equalTo(
            "---\n' topic': [\"leading\", \"next\"]\n"
                + "topic: [\"plain\", \"second\"]\n'topic ': trailing\n"
                + "' topic ': [\"doubled\"]\n' topic 2 ': untouched\n---\n"));
    assertThat(
        result.focuses().get(new PropertyFocus(" topic 2", "")),
        equalTo(new PropertyFocus(" topic", "next")));
  }

  @Test
  void ordersNumericSuffixesBeyondIntegerAndLongRanges() {
    var result =
        NoteContentMarkdown.consolidateNumberedProperties(
            "---\ntopic 9223372036854775808000: last\ntopic 2147483648: middle\n"
                + "topic 2: first\nimage 9223372036854775808000: keep\n---\n");
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        result.content(),
        equalTo(
            "---\ntopic: [\"first\", \"middle\", \"last\"]\n"
                + "image 9223372036854775808000: keep\n---\n"));
  }

  @Test
  void preservesCodecScalarMeaningsAndRepeatedRunIsUnchanged() {
    var result =
        NoteContentMarkdown.consolidateNumberedProperties(
            "---\ntopic: true\ntopic 2: [42, 'true', \"line\\nnext\"]\n---\n");
    assertThat(result.diagnostic(), nullValue());
    assertThat(
        NoteContentMarkdown.splitLeadingFrontmatter(result.content())
            .orElseThrow()
            .frontmatter()
            .getPropertyValue("topic"),
        equalTo(
            Optional.of(
                new FrontmatterPropertyValue.ListItems(List.of("true", "42", "line\nnext")))));
    var repeated = NoteContentMarkdown.consolidateNumberedProperties(result.content());
    assertThat(repeated.content(), equalTo(result.content()));
    assertThat(repeated.focuses(), equalTo(Map.of()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "topic: A\ntopic 2: {nested: value}\n",
        "topic 2: [A, [nested]]\n",
        "topic: null\ntopic 2: A\n",
        "topic 2: A\ntopic 2: B\n",
        "topic 2: &value A\nother: *value\n",
        "{topic: A, topic 2: B}\n"
      })
  void unsupportedOrAmbiguousShapesYieldOnlyDiagnostic(String yaml) {
    var result = NoteContentMarkdown.consolidateNumberedProperties("---\n" + yaml + "---\nBody");
    assertThat(result.diagnostic() != null, equalTo(true));
    assertThat(result.content(), nullValue());
    assertThat(result.focuses(), equalTo(Map.of()));
  }
}
