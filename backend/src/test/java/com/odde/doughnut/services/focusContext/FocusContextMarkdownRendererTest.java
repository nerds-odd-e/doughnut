package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FocusContextMarkdownRendererTest {

  private FocusContextMarkdownRenderer renderer;
  private RetrievalConfig config;

  @BeforeEach
  void setup() {
    renderer = new FocusContextMarkdownRenderer();
    config = RetrievalConfig.depth1();
  }

  @Nested
  class SafeFenceSelection {
    @Test
    void noteWithNoBackticksUses3Backticks() {
      assertThat(FocusContextMarkdownRenderer.safeFence("no backticks here"), equalTo("```"));
    }

    @Test
    void noteWithTripleBackticksUses4Backticks() {
      assertThat(
          FocusContextMarkdownRenderer.safeFence("some ```java\ncode\n```"), equalTo("````"));
    }

    @Test
    void noteWithQuadrupleBackticksUses5Backticks() {
      assertThat(FocusContextMarkdownRenderer.safeFence("has ```` fences"), equalTo("`````"));
    }

    @Test
    void emptyContentUses3Backticks() {
      assertThat(FocusContextMarkdownRenderer.safeFence(null), equalTo("```"));
      assertThat(FocusContextMarkdownRenderer.safeFence(""), equalTo("```"));
    }
  }

  @Nested
  class FocusNoteBlock {
    @Test
    void focusNoteAlwaysEmittedEvenWithNoRelatedNotes() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "My Notebook",
              "My Title",
              "",
              0,
              List.of(),
              List.of(),
              List.of(),
              null,
              "Some content",
              false);
      FocusContextResult result = new FocusContextResult(focusNote);

      String output = renderer.render(result, config);

      assertThat(output, containsString("## Focus Note"));
      assertThat(output, containsString("Title: My Title"));
      assertThat(output, containsString("Notebook: My Notebook"));
      assertThat(output, containsString("Depth: 0"));
      assertThat(output, containsString("```doughnut-note-md"));
      assertThat(output, containsString("Some content"));
    }

    @Test
    void truncationFlagRenderedWhenSet() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "NB",
              "Title",
              "",
              0,
              List.of(),
              List.of(),
              List.of(),
              null,
              "truncated content...",
              true);
      FocusContextResult result = new FocusContextResult(focusNote);

      String output = renderer.render(result, config);

      assertThat(output, containsString("Truncated: true"));
    }

    @Test
    void truncationFlagFalseWhenNotTruncated() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "NB", "Title", "", 0, List.of(), List.of(), List.of(), null, "full content", false);
      FocusContextResult result = new FocusContextResult(focusNote);

      String output = renderer.render(result, config);

      assertThat(output, containsString("Truncated: false"));
    }

    @Test
    void headerContainsMaxDepth() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              null, "T", "", 0, List.of(), List.of(), List.of(), null, null, false);
      FocusContextResult result = new FocusContextResult(focusNote);

      String output = renderer.render(result, config);

      assertThat(output, containsString("Max depth: 1"));
    }

    @Test
    void defaultRetrievalConfigMaxDepthIsTwo() {
      assertThat(RetrievalConfig.defaultMaxDepth().getMaxDepth(), equalTo(2));
    }
  }

  @Nested
  class RetrievedNoteBlock {
    @Test
    void retrievalPathFormattedCorrectlyForDepth1OutgoingLink() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "NB", "A", "", 0, List.of(), List.of(), List.of(), null, "focus", false);
      FocusContextResult result = new FocusContextResult(focusNote);
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

      String output = renderer.render(result, config);

      assertThat(output, containsString("## Retrieved Note"));
      assertThat(output, containsString("Path: [[A]] -> [[NB: B]]"));
      assertThat(output, containsString("Reached by: OutgoingWikiLink"));
      assertThat(output, containsString("content of B"));
    }

    @Test
    void emptyRelatedNotesProducesNoRetrievedNoteBlock() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              null, "T", "", 0, List.of(), List.of(), List.of(), null, null, false);
      FocusContextResult result = new FocusContextResult(focusNote);

      String output = renderer.render(result, config);

      assertThat(output, not(containsString("## Retrieved Note")));
    }

    @Test
    void depthTwoPathUsesArrowBetweenWikiUris() {
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "NB", "Focus", "", 0, List.of(), List.of(), List.of(), null, "focus body", false);
      FocusContextResult result = new FocusContextResult(focusNote);
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
      FocusContextFocusNote focusNote =
          new FocusContextFocusNote(
              "NB", "AnchorTitle", "", 0, List.of(), List.of(), List.of(), null, "focus", false);
      FocusContextResult result = new FocusContextResult(focusNote);
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

      String output = renderer.render(result, config);

      assertThat(output, containsString("Reached by: FolderSibling"));
      assertThat(output, containsString("Path: [[AnchorTitle]]"));
      assertThat(output, containsString("Depth: 1"));
    }
  }
}
