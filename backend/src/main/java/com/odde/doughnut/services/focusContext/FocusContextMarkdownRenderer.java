package com.odde.doughnut.services.focusContext;

import org.springframework.stereotype.Service;

@Service
public class FocusContextMarkdownRenderer {

  public String render(FocusContextResult result, RetrievalConfig config) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Doughnut Focus Context\n\n");
    sb.append("Purpose: Context around the focus note for AI use.\n");
    sb.append("Max depth: ").append(config.getMaxDepth()).append("\n");

    appendFocusNote(sb, result.getFocusNote());

    for (FocusContextNote note : result.getRelatedNotes()) {
      sb.append("\n---\n");
      appendRetrievedNote(sb, note);
    }

    return sb.toString();
  }

  private void appendFocusNote(StringBuilder sb, FocusContextFocusNote focusNote) {
    sb.append("\n## Focus Note\n\n");
    sb.append("Title: ")
        .append(focusNote.getTitle() != null ? focusNote.getTitle() : "")
        .append("\n");
    if (focusNote.getNotebook() != null) {
      sb.append("Notebook: ").append(focusNote.getNotebook()).append("\n");
    }
    if (focusNote.getFolderPath() != null && !focusNote.getFolderPath().isEmpty()) {
      sb.append("Folder: ").append(focusNote.getFolderPath()).append("\n");
    }
    sb.append("Depth: 0\n");
    sb.append("Truncated: ").append(focusNote.isDetailsTruncated()).append("\n");
    sb.append("\nContent:\n\n");
    appendFencedContent(sb, focusNote.getDetails());
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
    sb.append("Truncated: ").append(note.isDetailsTruncated()).append("\n");
    sb.append("\nContent:\n\n");
    appendFencedContent(sb, note.getDetails());
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
