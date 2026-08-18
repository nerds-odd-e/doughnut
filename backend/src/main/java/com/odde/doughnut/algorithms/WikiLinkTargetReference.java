package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Notebook name and note title used to resolve a wiki-link token to a target note. */
public record WikiLinkTargetReference(String notebookName, String noteTitle) {

  /**
   * Parses a wiki-link inner token into a notebook/title pair. Qualified links use {@code
   * Notebook:Title}; unqualified links use {@code focusNotebookName} as the notebook.
   */
  public static Optional<WikiLinkTargetReference> forToken(String token, String focusNotebookName) {
    String resolutionKey = WikiLinkMarkdown.splitInner(token).target();
    if (resolutionKey == null || resolutionKey.isBlank()) {
      return Optional.empty();
    }
    Qualified qualified = Qualified.tryParse(resolutionKey);
    if (qualified != null) {
      return Optional.of(
          new WikiLinkTargetReference(qualified.notebookName(), qualified.noteTitle()));
    }
    if (focusNotebookName == null || focusNotebookName.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new WikiLinkTargetReference(focusNotebookName, resolutionKey));
  }

  /**
   * Folder names then title for a path-shaped target ({@code Folder/Title}). Optional trailing
   * {@code .md} on the last segment is ignored. Unqualified titles are not path-shaped.
   */
  public record PathShapedTarget(List<String> folderNames, String title) {
    public static Optional<PathShapedTarget> tryParse(String noteTitle) {
      if (noteTitle == null || noteTitle.contains(":") || !noteTitle.contains("/")) {
        return Optional.empty();
      }
      List<String> segments = new ArrayList<>();
      for (String raw : noteTitle.split("/", -1)) {
        String segment = raw.trim();
        if (!segment.isEmpty()) {
          segments.add(segment);
        }
      }
      if (segments.size() < 2) {
        return Optional.empty();
      }
      String last = segments.getLast();
      String title = stripOptionalMarkdownSuffix(last);
      if (title.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(
          new PathShapedTarget(List.copyOf(segments.subList(0, segments.size() - 1)), title));
    }

    private static String stripOptionalMarkdownSuffix(String lastSegment) {
      if (lastSegment.length() > 3
          && lastSegment.regionMatches(true, lastSegment.length() - 3, ".md", 0, 3)) {
        return lastSegment.substring(0, lastSegment.length() - 3);
      }
      return lastSegment;
    }
  }

  private record Qualified(String notebookName, String noteTitle) {
    static Qualified tryParse(String token) {
      int i = token.indexOf(':');
      if (i <= 0 || i >= token.length() - 1) {
        return null;
      }
      String nb = token.substring(0, i).trim();
      String nt = token.substring(i + 1).trim();
      if (nb.isEmpty() || nt.isEmpty()) {
        return null;
      }
      return new Qualified(nb, nt);
    }
  }
}
