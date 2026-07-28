package com.odde.doughnut.services.notebookExport;

public final class NotebookExportFilenames {
  private static final String INVALID_CHARS_PATTERN = "[\\\\/:*?\"<>|\\x00-\\x1F]";

  private NotebookExportFilenames() {}

  public static String sanitize(String raw) {
    String base = raw == null ? "" : raw;
    String collapsed = base.replaceAll(INVALID_CHARS_PATTERN, " ").trim().replaceAll("\\s+", " ");
    return collapsed.isEmpty() ? "Untitled" : collapsed;
  }
}
