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

  @ParameterizedTest(name = "{0}")
  @MethodSource("directContentCases")
  void hasDirectContent_matrix(String ignoredLabel, List<BlockSpec> specs, boolean expected) {
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

  static Stream<Arguments> directContentCases() {
    return Stream.of(
        Arguments.of("empty list", List.of(), false),
        Arguments.of(
            "body text (no text_level)",
            List.of(new BlockSpec("text", "{\"type\":\"text\",\"text\":\"hello\"}")),
            true),
        Arguments.of(
            "body text (text_level 0)",
            List.of(new BlockSpec("text", "{\"type\":\"text\",\"text_level\":0,\"text\":\"x\"}")),
            true),
        Arguments.of(
            "table",
            List.of(
                new BlockSpec("table", "{\"type\":\"table\",\"table_body\":\"<table></table>\"}")),
            true),
        Arguments.of(
            "image",
            List.of(new BlockSpec("image", "{\"type\":\"image\",\"img_path\":\"x.png\"}")),
            true),
        Arguments.of(
            "page_number only",
            List.of(new BlockSpec("page_number", "{\"type\":\"page_number\",\"text\":\"1\"}")),
            false),
        Arguments.of(
            "page_footnote only",
            List.of(new BlockSpec("page_footnote", "{\"type\":\"page_footnote\",\"text\":\"fn\"}")),
            false),
        Arguments.of(
            "hypothetical page_* only",
            List.of(
                new BlockSpec("page_header", "{\"type\":\"page_header\",\"text\":\"running\"}")),
            false),
        Arguments.of(
            "header only",
            List.of(new BlockSpec("header", "{\"type\":\"header\",\"text\":\"book title\"}")),
            false),
        Arguments.of(
            "footer only",
            List.of(new BlockSpec("footer", "{\"type\":\"footer\",\"text\":\"copyright\"}")),
            false),
        Arguments.of(
            "unknown type",
            List.of(new BlockSpec("equation", "{\"type\":\"equation\",\"text\":\"E=mc^2\"}")),
            false),
        Arguments.of(
            "legacy heading-shaped text row (text_level 2) only",
            List.of(
                new BlockSpec(
                    "text",
                    "{\"type\":\"text\",\"text_level\":2,\"text\":\"2.1 A heading\",\"page_idx\":0}")),
            false),
        Arguments.of(
            "heading-shaped text then body text — body counts",
            List.of(
                new BlockSpec(
                    "text",
                    "{\"type\":\"text\",\"text_level\":2,\"text\":\"Section\",\"page_idx\":0}"),
                new BlockSpec("text", "{\"type\":\"text\",\"text\":\"paragraph\"}")),
            true),
        Arguments.of(
            "page_number then table — table counts",
            List.of(
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
