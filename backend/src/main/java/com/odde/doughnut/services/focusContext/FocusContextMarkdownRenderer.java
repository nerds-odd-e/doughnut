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
    appendTitleNotebookFolderDepth(
        sb,
        focusNote.getTitle(),
        focusNote.getNotebook(),
        focusNote.getFolderPath(),
        focusNote.getDepth());
    appendTruncationAndContent(sb, focusNote.isContentTruncated(), focusNote.getContent());
    sb.append(FocusContextConstants.FOCUS_NOTE_CLOSE_TAG);
  }

  private void appendRetrievedNote(StringBuilder sb, FocusContextNote note) {
    sb.append(FocusContextConstants.RETRIEVED_NOTE_SECTION_START);
    appendTitleNotebookFolderDepth(
        sb, note.getTitle(), note.getNotebook(), note.getFolderPath(), note.getDepth());
    if (note.getRetrievalPath() != null && !note.getRetrievalPath().isEmpty()) {
      sb.append("Path: ").append(String.join(" -> ", note.getRetrievalPath())).append("\n");
    }
    appendTruncationAndContent(sb, note.isContentTruncated(), note.getContent());
    sb.append(FocusContextConstants.RETRIEVED_NOTE_CLOSE_TAG);
  }

  private static void appendTitleNotebookFolderDepth(
      StringBuilder sb, String title, String notebook, String folderPath, int depth) {
    sb.append("Title: ").append(title != null ? title : "").append("\n");
    if (notebook != null) {
      sb.append("Notebook: ").append(notebook).append("\n");
    }
    if (folderPath != null && !folderPath.isEmpty()) {
      sb.append("Folder: ").append(folderPath).append("\n");
    }
    sb.append("Depth: ").append(depth).append("\n");
  }

  private void appendTruncationAndContent(
      StringBuilder sb, boolean contentTruncated, String content) {
    if (contentTruncated) {
      sb.append("Truncated: true\n");
    }
    if (hasRenderableContent(content)) {
      sb.append("\nContent:\n\n");
      appendFencedContent(sb, content);
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
