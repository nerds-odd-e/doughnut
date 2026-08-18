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
    String resolutionKey = WikiLinkMarkdown.splitAuthoredToken(token).target();
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

  static boolean isQualifiedToken(String targetToken) {
    return Qualified.tryParse(targetToken) != null;
  }

  static String replaceNoteTitle(String targetToken, String newTitle) {
    Qualified qualified = Qualified.tryParse(targetToken);
    if (qualified != null) {
      return qualified.notebookName() + ":" + newTitle;
    }
    return PathShapedTarget.tryParse(targetToken)
        .map(path -> path.withNoteTitle(newTitle))
        .orElse(newTitle);
  }

  static String replaceNotebookName(String targetToken, String newNotebookName) {
    Qualified qualified = Qualified.tryParse(targetToken);
    String noteTitle = qualified == null ? targetToken : qualified.noteTitle();
    return newNotebookName + ":" + noteTitle;
  }

  /**
   * Folder names then title for a path-shaped target ({@code Folder/Title}, or Markdown {@code
   * /Title} at notebook root). Optional trailing {@code .md} on the last segment is ignored.
   * Unqualified titles without a leading {@code /} are not path-shaped.
   */
  public record PathShapedTarget(List<String> folderNames, String title) {
    public static Optional<PathShapedTarget> tryParse(String noteTitle) {
      if (noteTitle == null || noteTitle.contains(":")) {
        return Optional.empty();
      }
      boolean leadingSlash = noteTitle.startsWith("/");
      if (!leadingSlash && !noteTitle.contains("/")) {
        return Optional.empty();
      }
      List<String> segments = new ArrayList<>();
      for (String raw : noteTitle.split("/", -1)) {
        String segment = raw.trim();
        if (!segment.isEmpty()) {
          segments.add(segment);
        }
      }
      if (segments.isEmpty()) {
        return Optional.empty();
      }
      if (segments.size() < 2 && !leadingSlash) {
        return Optional.empty();
      }
      String last = segments.getLast();
      String title = stripOptionalMarkdownSuffix(last);
      if (title.isEmpty()) {
        return Optional.empty();
      }
      List<String> folderNames =
          segments.size() == 1 ? List.of() : List.copyOf(segments.subList(0, segments.size() - 1));
      return Optional.of(new PathShapedTarget(folderNames, title));
    }

    String withNoteTitle(String newTitle) {
      return String.join("/", folderNames) + "/" + newTitle;
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
