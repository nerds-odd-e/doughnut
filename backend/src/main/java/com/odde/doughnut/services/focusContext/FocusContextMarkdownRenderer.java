package com.odde.doughnut.services.focusContext;

import org.springframework.stereotype.Service;

@Service
public class FocusContextMarkdownRenderer {

  public String render(FocusContextResult result, RetrievalConfig config) {
    StringBuilder sb = new StringBuilder();

    sb.append(FocusContextConstants.FOCUS_CONTEXT_OPEN_TAG);
    sb.append("Purpose: Context around the focus note for AI use.\n");
    sb.append("Max depth: ").append(config.getMaxDepth()).append("\n");

    appendFocusNote(sb, result.getFocusNote());

    for (FocusContextNote note : result.getRelatedNotes()) {
      sb.append("\n---\n");
      appendRetrievedNote(sb, note);
    }

    sb.append(FocusContextConstants.FOCUS_CONTEXT_CLOSE_TAG);
    return sb.toString();
  }

  private void appendFocusNote(StringBuilder sb, FocusContextFocusNote focusNote) {
    sb.append(FocusContextConstants.FOCUS_NOTE_SECTION_START);
    sb.append("Title: ")
        .append(focusNote.getTitle() != null ? focusNote.getTitle() : "")
        .append("\n");
    if (focusNote.getNotebook() != null) {
      sb.append("Notebook: ").append(focusNote.getNotebook()).append("\n");
    }
    if (focusNote.getFolderPath() != null && !focusNote.getFolderPath().isEmpty()) {
      sb.append("Folder: ").append(focusNote.getFolderPath()).append("\n");
    }
    sb.append("Depth: ").append(focusNote.getDepth()).append("\n");
    if (focusNote.isContentTruncated()) {
      sb.append("Truncated: true\n");
    }
    if (hasRenderableContent(focusNote.getContent())) {
      sb.append("\nContent:\n\n");
      appendFencedContent(sb, focusNote.getContent());
    }
    sb.append(FocusContextConstants.FOCUS_NOTE_CLOSE_TAG);
  }

  private void appendRetrievedNote(StringBuilder sb, FocusContextNote note) {
    sb.append("\n## Retrieved Note\n\n");
    sb.append("Title: ").append(note.getTitle() != null ? note.getTitle() : "").append("\n");
    if (note.getNotebook() != null) {
      sb.append("Notebook: ").append(note.getNotebook()).append("\n");
    }
    if (note.getFolderPath() != null && !note.getFolderPath().isEmpty()) {
      sb.append("Folder: ").append(note.getFolderPath()).append("\n");
    }
    sb.append("Depth: ").append(note.getDepth()).append("\n");
    if (note.getRetrievalPath() != null && !note.getRetrievalPath().isEmpty()) {
      sb.append("Path: ").append(String.join(" -> ", note.getRetrievalPath())).append("\n");
    }
    if (note.getEdgeType() != null) {
      sb.append("Reached by: ").append(note.getEdgeType()).append("\n");
    }
    if (note.isContentTruncated()) {
      sb.append("Truncated: true\n");
    }
    if (hasRenderableContent(note.getContent())) {
      sb.append("\nContent:\n\n");
      appendFencedContent(sb, note.getContent());
    }
  }

  private static boolean hasRenderableContent(String content) {
    return content != null && !content.isBlank();
  }

  private void appendFencedContent(StringBuilder sb, String content) {
    String fence = safeFence(content);
    sb.append(fence).append("doughnut-note-md\n");
    if (content != null) {
      sb.append(content);
      if (!content.endsWith("\n")) {
        sb.append("\n");
      }
    }
    sb.append(fence).append("\n");
  }

  static String safeFence(String content) {
    int longestRun = longestBacktickRun(content);
    int fenceLength = Math.max(3, longestRun + 1);
    return "`".repeat(fenceLength);
  }

  private static int longestBacktickRun(String content) {
    if (content == null || content.isEmpty()) {
      return 0;
    }
    int longest = 0;
    int current = 0;
    for (char c : content.toCharArray()) {
      if (c == '`') {
        current++;
        if (current > longest) {
          longest = current;
        }
      } else {
        current = 0;
      }
    }
    return longest;
  }
}
