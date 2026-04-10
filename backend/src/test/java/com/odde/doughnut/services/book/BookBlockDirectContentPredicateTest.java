package com.odde.doughnut.services.book;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.BookContentBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BookBlockDirectContentPredicateTest {

  private static BookContentBlock block(String type, String rawJson) {
    BookContentBlock cb = new BookContentBlock();
    cb.setType(type);
    cb.setRawData(rawJson);
    return cb;
  }

  private static final BlockSpec HEADING =
      new BlockSpec(
          "text",
          "{\"type\":\"text\",\"text_level\":1,\"text\":\"Chapter\",\"page_idx\":0,\"bbox\":[0,0,100,20]}");

  @ParameterizedTest(name = "{0}")
  @MethodSource("contributesDirectContentCases")
  void contributesDirectContent_matrix(String ignoredLabel, BlockSpec spec, boolean expected) {
    BookContentBlock cb = block(spec.type(), spec.rawJson());
    assertThat(BookBlockDirectContentPredicate.contributesDirectContent(cb), equalTo(expected));
  }

  static Stream<Arguments> contributesDirectContentCases() {
    return Stream.of(
        Arguments.of(
            "body text (no text_level)",
            new BlockSpec("text", "{\"type\":\"text\",\"text\":\"hello\"}"),
            true),
        Arguments.of(
            "body text (text_level 0)",
            new BlockSpec("text", "{\"type\":\"text\",\"text_level\":0,\"text\":\"x\"}"),
            true),
        Arguments.of(
            "table",
            new BlockSpec("table", "{\"type\":\"table\",\"table_body\":\"<table></table>\"}"),
            true),
        Arguments.of(
            "image", new BlockSpec("image", "{\"type\":\"image\",\"img_path\":\"x.png\"}"), true),
        Arguments.of(
            "page_number",
            new BlockSpec("page_number", "{\"type\":\"page_number\",\"text\":\"1\"}"),
            false),
        Arguments.of(
            "page_footnote",
            new BlockSpec("page_footnote", "{\"type\":\"page_footnote\",\"text\":\"fn\"}"),
            false),
        Arguments.of(
            "page_*",
            new BlockSpec("page_header", "{\"type\":\"page_header\",\"text\":\"running\"}"),
            false),
        Arguments.of(
            "header",
            new BlockSpec("header", "{\"type\":\"header\",\"text\":\"book title\"}"),
            false),
        Arguments.of(
            "footer",
            new BlockSpec("footer", "{\"type\":\"footer\",\"text\":\"copyright\"}"),
            false),
        Arguments.of(
            "unknown type",
            new BlockSpec("equation", "{\"type\":\"equation\",\"text\":\"E=mc^2\"}"),
            true),
        Arguments.of(
            "heading-shaped text (text_level 2)",
            new BlockSpec(
                "text",
                "{\"type\":\"text\",\"text_level\":2,\"text\":\"2.1 A heading\",\"page_idx\":0}"),
            false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("hasDirectContentCases")
  void hasDirectContent_skipsFirstElement(
      String ignoredLabel, List<BlockSpec> specs, boolean expected) {
    List<BookContentBlock> blocks = new ArrayList<>();
    for (int i = 0; i < specs.size(); i++) {
      BlockSpec s = specs.get(i);
      BookContentBlock cb = new BookContentBlock();
      cb.setSiblingOrder(i);
      cb.setType(s.type());
      cb.setRawData(s.rawJson());
      blocks.add(cb);
    }
    assertThat(BookBlockDirectContentPredicate.hasDirectContent(blocks), equalTo(expected));
  }

  static Stream<Arguments> hasDirectContentCases() {
    return Stream.of(
        Arguments.of("empty list", List.of(), false),
        Arguments.of("heading only (index 0 skipped)", List.of(HEADING), false),
        Arguments.of(
            "heading + body text — body counts",
            List.of(HEADING, new BlockSpec("text", "{\"type\":\"text\",\"text\":\"paragraph\"}")),
            true),
        Arguments.of(
            "heading + table — table counts",
            List.of(
                HEADING,
                new BlockSpec("table", "{\"type\":\"table\",\"table_body\":\"<table></table>\"}")),
            true),
        Arguments.of(
            "heading + image — image counts",
            List.of(HEADING, new BlockSpec("image", "{\"type\":\"image\",\"img_path\":\"x.png\"}")),
            true),
        Arguments.of(
            "heading + page_number only — page_number does not count",
            List.of(
                HEADING, new BlockSpec("page_number", "{\"type\":\"page_number\",\"text\":\"1\"}")),
            false),
        Arguments.of(
            "heading + header only — header does not count",
            List.of(
                HEADING, new BlockSpec("header", "{\"type\":\"header\",\"text\":\"book title\"}")),
            false),
        Arguments.of(
            "heading + heading-shaped body (text_level 2) — does not count",
            List.of(
                HEADING,
                new BlockSpec(
                    "text",
                    "{\"type\":\"text\",\"text_level\":2,\"text\":\"2.1 sub\",\"page_idx\":0}")),
            false),
        Arguments.of(
            "heading + heading-shaped body + plain body — plain body counts",
            List.of(
                HEADING,
                new BlockSpec(
                    "text",
                    "{\"type\":\"text\",\"text_level\":2,\"text\":\"Section\",\"page_idx\":0}"),
                new BlockSpec("text", "{\"type\":\"text\",\"text\":\"paragraph\"}")),
            true),
        Arguments.of(
            "heading + page_number + table — table counts",
            List.of(
                HEADING,
                new BlockSpec("page_number", "{\"type\":\"page_number\",\"text\":\"1\"}"),
                new BlockSpec("table", "{\"type\":\"table\",\"table_body\":\"<table></table>\"}")),
            true));
  }

  @Test
  void textLevelFromRaw_invalidJson_returnsNull() {
    assertThat(BookBlockDirectContentPredicate.textLevelFromRaw("not json"), equalTo(null));
  }

  record BlockSpec(String type, String rawJson) {}
}
