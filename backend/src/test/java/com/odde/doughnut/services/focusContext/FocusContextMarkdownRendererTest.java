package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FocusContextMarkdownRendererTest {

  private FocusContextMarkdownRenderer renderer;
  private RetrievalConfig depth1Config;

  @BeforeEach
  void setup() {
    renderer = new FocusContextMarkdownRenderer();
    depth1Config = RetrievalConfig.depth1();
  }

  private static FocusContextFocusNote focusNote(
      String notebook, String title, String details, boolean detailsTruncated) {
    return new FocusContextFocusNote(
        notebook, title, "", 0, List.of(), List.of(), List.of(), null, details, detailsTruncated);
  }

  @Nested
  class SafeFenceSelection {
    @Test
    void safeFenceEscalatesWithLongestRunOfBackticksInContent() {
      assertThat(FocusContextMarkdownRenderer.safeFence("no backticks here"), equalTo("```"));
      assertThat(
          FocusContextMarkdownRenderer.safeFence("some ```java\ncode\n```"), equalTo("````"));
      assertThat(FocusContextMarkdownRenderer.safeFence("has ```` fences"), equalTo("`````"));
      assertThat(FocusContextMarkdownRenderer.safeFence(null), equalTo("```"));
      assertThat(FocusContextMarkdownRenderer.safeFence(""), equalTo("```"));
    }
  }

  @Nested
  class FocusNoteBlock {
    @Test
    void focusNoteSectionIncludesMetadataFenceAndOmitsRetrievedBlockWhenNoRelated() {
      FocusContextFocusNote focus = focusNote("My Notebook", "My Title", "Some content", false);
      FocusContextResult result = new FocusContextResult(focus);

      String output = renderer.render(result, depth1Config);

      assertThat(output, containsString("## Focus Note"));
      assertThat(output, containsString("Title: My Title"));
      assertThat(output, containsString("Notebook: My Notebook"));
      assertThat(output, containsString("Depth: 0"));
      assertThat(output, containsString("Max depth: 1"));
      assertThat(output, containsString("Truncated: false"));
      assertThat(output, containsString("```doughnut-note-md"));
      assertThat(output, containsString("Some content"));
      assertThat(output, not(containsString("## Retrieved Note")));
    }

    @Test
    void truncationFlagRenderedForTrueAndFalse() {
      String truncatedOut =
          renderer.render(
              new FocusContextResult(focusNote("NB", "T", "short…", true)), depth1Config);
      assertThat(truncatedOut, containsString("Truncated: true"));

      String fullOut =
          renderer.render(
              new FocusContextResult(focusNote("NB", "T", "full content", false)), depth1Config);
      assertThat(fullOut, containsString("Truncated: false"));
    }
  }

  @Nested
  class RetrievedNoteBlock {
    @Test
    void depth1OutgoingLinkFormatsPathEdgeAndBody() {
      FocusContextResult result = new FocusContextResult(focusNote("NB", "A", "focus", false));
      result.addRelatedNote(
          new FocusContextNote(
              "NB",
              "B",
              "",
              1,
              List.of("[[A]]", "[[NB: B]]"),
              FocusContextEdgeType.OutgoingWikiLink,
              null,
              "content of B",
              false));

      String output = renderer.render(result, depth1Config);

      assertThat(output, containsString("## Retrieved Note"));
      assertThat(output, containsString("Path: [[A]] -> [[NB: B]]"));
      assertThat(output, containsString("Reached by: OutgoingWikiLink"));
      assertThat(output, containsString("content of B"));
    }

    @Test
    void depthTwoOutgoingFormatsPathTruncationAndDefaultMaxDepthHeader() {
      FocusContextResult result =
          new FocusContextResult(focusNote("NB", "Focus", "focus body", false));
      result.addRelatedNote(
          new FocusContextNote(
              "NB",
              "Far",
              "",
              2,
              List.of("[[Focus]]", "[[NB: Mid]]", "[[NB: Far]]"),
              FocusContextEdgeType.OutgoingWikiLink,
              null,
              "far details",
              true));

      String output = renderer.render(result, RetrievalConfig.defaultMaxDepth());

      assertThat(output, containsString("Max depth: 2"));
      assertThat(output, containsString("Depth: 2"));
      assertThat(output, containsString("Path: [[Focus]] -> [[NB: Mid]] -> [[NB: Far]]"));
      assertThat(output, containsString("Truncated: true"));
    }

    @Test
    void folderSiblingShowsPathToAnchorAndEdgeType() {
      FocusContextResult result =
          new FocusContextResult(focusNote("NB", "AnchorTitle", "focus", false));
      result.addRelatedNote(
          new FocusContextNote(
              "NB",
              "Peer",
              "",
              1,
              List.of("[[AnchorTitle]]"),
              FocusContextEdgeType.FolderSibling,
              null,
              "peer body",
              false));

      String output = renderer.render(result, depth1Config);

      assertThat(output, containsString("Reached by: FolderSibling"));
      assertThat(output, containsString("Path: [[AnchorTitle]]"));
      assertThat(output, containsString("Depth: 1"));
    }
  }
}
