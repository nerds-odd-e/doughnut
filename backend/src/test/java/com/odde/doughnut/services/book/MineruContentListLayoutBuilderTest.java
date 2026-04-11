package com.odde.doughnut.services.book;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MineruContentListLayoutBuilderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @ParameterizedTest(name = "{0}")
  @MethodSource("bboxCases")
  void isValidBbox_matrix(String ignored, Object bbox, boolean expected) {
    assertThat(MineruContentListLayoutBuilder.isValidBbox(bbox), equalTo(expected));
  }

  static Stream<Arguments> bboxCases() {
    return Stream.of(
        Arguments.of("valid", List.of(0.0, 10.0, 100.0, 200.0), true),
        Arguments.of("x0 equals x1", List.of(50.0, 10.0, 50.0, 200.0), false),
        Arguments.of("x0 greater than x1", List.of(100.0, 10.0, 50.0, 200.0), false),
        Arguments.of("y0 equals y1", List.of(0.0, 100.0, 100.0, 100.0), false),
        Arguments.of("y0 greater than y1", List.of(0.0, 200.0, 100.0, 100.0), false),
        Arguments.of("null", null, false),
        Arguments.of("not a list", "0,0,1,1", false),
        Arguments.of("wrong length", List.of(1.0, 2.0, 3.0), false),
        Arguments.of("non-numeric", List.of(0, 0, "a", 1), false),
        Arguments.of("infinite", List.of(0.0, 0.0, Double.POSITIVE_INFINITY, 1.0), false),
        Arguments.of("nan", List.of(0.0, Double.NaN, 100.0, 1.0), false));
  }

  @Test
  void orphanPrefixThenHeadingYieldsBeginningFirst() {
    List<Map<String, Object>> data = new ArrayList<>();
    data.add(textBody(0, List.of(10, 100, 200, 130), "Orphan body"));
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("type", "text");
    h.put("text", "Chapter One");
    h.put("text_level", 2);
    h.put("bbox", List.of(1, 200, 300, 240));
    h.put("page_idx", 1);
    data.add(h);

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), hasSize(2));
    assertThat(layout.getRoots().getFirst().getTitle(), equalTo("*beginning*"));
    assertThat(layout.getRoots().getFirst().getContentBlocks(), hasSize(2));
    assertThat(
        layout.getRoots().getFirst().getContentBlocks().getFirst().get("type"),
        equalTo("beginning_anchor"));
    assertThat(
        layout.getRoots().getFirst().getContentBlocks().get(1).get("text"), equalTo("Orphan body"));
    assertThat(layout.getRoots().get(1).getTitle(), equalTo("Chapter One"));
    assertThat(layout.getRoots().get(1).getContentBlocks(), hasSize(1));
    assertThat(
        layout.getRoots().get(1).getContentBlocks().getFirst().get("text"), equalTo("Chapter One"));
  }

  @Test
  void syntheticBboxOneLineAboveFirstOrphan() {
    List<Map<String, Object>> data =
        List.of(
            textBody(0, List.of(10.0, 100.0, 20.0, 130.0), "x"),
            heading(1, 0, List.of(0, 0, 1, 1), "H"));

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    Map<String, Object> anchor = layout.getRoots().getFirst().getContentBlocks().getFirst();
    assertThat(anchor.get("type"), equalTo("beginning_anchor"));
    assertThat(anchor.get("page_idx"), equalTo(0));
    assertThat(anchor.get("kind"), equalTo("beginning"));
    assertThat(anchor.get("bbox"), equalTo(List.of(10.0, 70.0, 20.0, 100.0)));
  }

  @Test
  void syntheticBboxClampsY0AtZero() {
    List<Map<String, Object>> data =
        List.of(textBody(0, List.of(0, 15, 100, 40), "x"), heading(1, 0, List.of(0, 0, 1, 1), "H"));

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    Map<String, Object> anchor = layout.getRoots().getFirst().getContentBlocks().getFirst();
    assertThat(anchor.get("bbox"), equalTo(List.of(0.0, 0.0, 100.0, 15.0)));
  }

  @Test
  void noOrphanPrefixHeadingIsFirstContentBlock() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("type", "text");
    h.put("text", "Code Refactoring");
    h.put("text_level", 2);
    h.put("bbox", List.of(90, 72, 576, 115));
    h.put("page_idx", 0);
    data.add(h);
    data.add(textBody(0, List.of(87, 135, 897, 219), "Refactoring is often explained"));

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), hasSize(1));
    AttachBookLayoutNodeRequest root = layout.getRoots().getFirst();
    assertThat(root.getTitle(), equalTo("Code Refactoring"));
    assertThat(root.getContentBlocks(), hasSize(2));
    assertThat(root.getContentBlocks().getFirst().get("text"), equalTo("Code Refactoring"));
    assertThat(
        root.getContentBlocks().get(1).get("text"), equalTo("Refactoring is often explained"));
  }

  @Test
  void onlyOrphanWithValidBboxCreatesBeginningRoot() {
    List<Map<String, Object>> data = List.of(textBody(0, List.of(10, 100, 200, 130), "intro"));

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), hasSize(1));
    assertThat(layout.getRoots().getFirst().getTitle(), equalTo("*beginning*"));
    assertThat(layout.getRoots().getFirst().getContentBlocks(), hasSize(2));
    assertThat(
        layout.getRoots().getFirst().getContentBlocks().getFirst().get("type"),
        equalTo("beginning_anchor"));
    assertThat(
        layout.getRoots().getFirst().getContentBlocks().get(1).get("text"), equalTo("intro"));
  }

  @Test
  void orphanWithoutBboxIgnoredYieldsEmptyRoots() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> pn = new LinkedHashMap<>();
    pn.put("type", "page_number");
    pn.put("text", "1");
    pn.put("page_idx", 0);
    data.add(pn);

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), empty());
  }

  @Test
  void headingWithoutBboxFilteredOut() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> h = new LinkedHashMap<>();
    h.put("type", "text");
    h.put("text", "Chapter One");
    h.put("text_level", 1);
    h.put("page_idx", 0);
    data.add(h);

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), empty());
  }

  @Test
  void beginningNodeNotCreatedWhenOrphanHasNoBboxThenHeading() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> pn = new LinkedHashMap<>();
    pn.put("type", "page_number");
    pn.put("text", "1");
    pn.put("page_idx", 0);
    data.add(pn);
    data.add(heading(1, 0, List.of(0, 50, 200, 80), "Chapter One"));

    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(data);
    assertThat(layout.getRoots(), hasSize(1));
    assertThat(layout.getRoots().getFirst().getTitle(), equalTo("Chapter One"));
  }

  @Test
  void refactoringFixtureStartsWithHeadingNoBeginningRoot() throws Exception {
    Path fixture =
        Path.of("")
            .toAbsolutePath()
            .getParent()
            .resolve("e2e_test/fixtures/book_reading/mineru_output_for_refactoring.json");
    List<Object> raw = MAPPER.readValue(fixture.toFile(), new TypeReference<>() {});
    AttachBookLayoutRequest layout = MineruContentListLayoutBuilder.buildLayout(raw);
    assertThat(layout.getRoots(), not(empty()));
    assertThat(layout.getRoots().getFirst().getTitle(), equalTo("Code Refactoring"));
  }

  private static Map<String, Object> textBody(
      int pageIdx, List<? extends Number> bbox, String text) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "text");
    m.put("text", text);
    m.put("page_idx", pageIdx);
    m.put("bbox", new ArrayList<>(bbox));
    return m;
  }

  private static Map<String, Object> heading(
      int level, int pageIdx, List<? extends Number> bbox, String text) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "text");
    m.put("text", text);
    m.put("text_level", level);
    m.put("page_idx", pageIdx);
    m.put("bbox", new ArrayList<>(bbox));
    return m;
  }
}
