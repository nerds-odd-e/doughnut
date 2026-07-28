package com.odde.doughnut.services.notebookExport;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NotebookExportFilenames {
  private static final String INVALID_CHARS_PATTERN = "[\\\\/:*?\"<>|\\x00-\\x1F]";

  private NotebookExportFilenames() {}

  public static String sanitize(String raw) {
    String base = raw == null ? "" : raw;
    String collapsed = base.replaceAll(INVALID_CHARS_PATTERN, " ").trim().replaceAll("\\s+", " ");
    return collapsed.isEmpty() ? "Untitled" : collapsed;
  }

  public static Map<Integer, String> uniqueFileNames(
      List<Map.Entry<Integer, String>> idsAndRawNames, String extension) {
    Map<Integer, String> result = new LinkedHashMap<>();
    Set<String> used = new HashSet<>();
    for (Map.Entry<Integer, String> entry : idsAndRawNames) {
      String base = sanitize(entry.getValue());
      String candidate = base + extension;
      if (used.contains(candidate)) {
        candidate = base + " (" + entry.getKey() + ")" + extension;
      }
      used.add(candidate);
      result.put(entry.getKey(), candidate);
    }
    return result;
  }
}
